package inrae.application.discovery.table.util

import inrae.semantic_web.rdf.{Literal, QueryVariable, SparqlBuilder, URI}
import inrae.semantic_web.{LazyFutureJsonValue, SW, StatementConfiguration}
import wvlet.log.Logger.rootLogger.{error, info}

import scala.concurrent.Future

case class RequestSemanticDb(endpoint: String, method: String = "POST_ENCODED", `type`: String = "tps") {

  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global

  val config: StatementConfiguration = new StatementConfiguration()
  config.setConfigString(
    """
      {
       "sources" : [{
         "id"     : "current",
         "url"    : """" + endpoint +
      """",
         "type"   : """" + `type` +
      """",
         "method" : """" + method +
      """"
       }],
       "settings" : {
         "logLevel" : "off",
         "sizeBatchProcessing" : 10,
         "cache" : true,
         "driver" : "inrae.semantic_web.driver.RosHTTPDriver",
         "pageSize" : 20
       } }
      """.stripMargin)


  def getEntities(): Future[List[(URI, String)]] = {
    SW(config).something("instance")
      .datatype(URI("label", "rdfs"), "label")
      .isSubjectOf(URI("a"))
      .set(URI("Class", "owl"))
      .select(List("instance", "label"))
      .map(response => response("results")("bindings").arr.map(r => {
        val uri = SparqlBuilder.createUri(r("instance"))
        try {
          (uri, SparqlBuilder.createLiteral(response("results")("datatypes")("label")(uri.toString)(0)).toString)
        } catch {
          case _: Throwable => (uri, uri.naiveLabel())
        }
      }).toList)
  }

  def getAttributes(selectedEntity: URI): Future[List[(URI, String)]] = {
    info(" -- getAttributes --")
    info("uri:" + selectedEntity.toString())
    val query = SW(config).something("attributeProperty")
      .datatype(URI("label", "rdfs"), "label")
      .isA(URI("DatatypeProperty", "owl"))
      /* .focus("attributeProperty")
        .isSubjectOf(URI("range","rdfs"))
          .set(selectedEntity) */
      .root()
      .something("instance")
      .isA(selectedEntity)
      .isSubjectOf(QueryVariable("attributeProperty"))
      .select(List("attributeProperty", "label"))

    query.map(
      response => {
        response("results")("bindings").arr.map(r => {

          val uri = SparqlBuilder.createUri(r("attributeProperty"))
          //val uri = r("attributeProperty")("value").toString
          val label = uri.naiveLabel()
          try {
            (uri, SparqlBuilder.createLiteral(response("results")("datatypes")("label")(uri.toString)(0)).toString)
          } catch {
            case e: Throwable => {
              error(e.getMessage)
              (uri, label)
            }
          }
        }
        )
      }.toList)
  }

  def getValues(entity: URI, attributes: List[URI]): Future[Map[URI, Map[URI, Literal]]] = {
    info(" -- getValues --")
    var query = SW(config).something("instance")
      .isA(entity)

    attributes.foreach(attribute => {
      query = query.focus("instance").isSubjectOf(attribute, attribute.naiveLabel())
    })

    query = query.focus("instance").datatype(URI("label", "rdfs"), "label_instance")
    query = query.focus("instance").datatype(URI("https://metabohub.peakforest.org/ontology/property#version"), "test_version")

    query.selectByPage(List("instance", "label_instance", "test_version") ++ attributes.map(_.naiveLabel())).flatMap(
      (res) => {
        val nb: Int = res._1
        val lFutureJsonValue: Seq[LazyFutureJsonValue] = res._2

        val ret: Future[Map[URI, Map[URI, Literal]]] = nb match {
          case n if (n > 0) => {
            lFutureJsonValue(0).wrapped.map(response => {

              response("results")("bindings").arr.map(row => {
                val uriInstance = SparqlBuilder.createUri(row("instance"))

                (uriInstance -> (attributes.map(
                  uri => {
                    (uri -> SparqlBuilder.createLiteral(row(uri.naiveLabel())))
                  }
                ) ++ {
                  val labelInstance = SparqlBuilder.createLiteral(response("results")("datatypes")("label_instance")(uriInstance.localName).arr.head)
                  List(uriInstance -> labelInstance)
                }).toMap)
              }).toMap
            })
          }
          //case _ => Map()
        }
        ret
      })
  }

}
