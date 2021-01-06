package inrae.application.discovery.table.util

import inrae.semantic_web.rdf.{Literal, QueryVariable, SparqlBuilder, URI}
import inrae.semantic_web.{LazyFutureJsonValue, SW, StatementConfiguration}
import wvlet.log.Logger.rootLogger.{error, info}

import scala.concurrent.Future

case class RequestSemanticDb(endpoint: String, method: String = "POST", `type`: String = "tps") {

  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global

  val config: StatementConfiguration = StatementConfiguration()
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

  def getLazyPagesValues(entity: URI, listFilters : Seq[(URI,String,Literal)], attributes: List[URI]): Future[(Int,Seq[LazyFutureJsonValue])]= {
    info(" -- getValues --")

    var query = SW(config).something("instance")
      .isA(entity)


    attributes.foreach(attribute => {
      query = query.focus("instance").isSubjectOf(attribute, attribute.naiveLabel())
      /* add filter contains */
      listFilters.filter( _._1.toString() == attribute.toString() ) foreach {
        case (_, "contains", regex ) => query = query.filter.contains(regex.value)
        case (_, "<", operand )  => query = query.filter.inf(operand)
        case (_, "<=", operand )  => query = query.filter.infEqual(operand)
        case (_, ">", operand )  => query = query.filter.sup(operand)
        case (_, ">=", operand ) => query = query.filter.supEqual(operand)
        case (_, "=", operand )  => query = query.filter.equal(operand)
        case (_, "<>", operand )  => query = query.filter.notEqual(operand)
        case a => println("unknown :"+a.toString)
      }
    })

    query = query.focus("instance").datatype(URI("label", "rdfs"), "label_instance")
    query.selectByPage(List("instance", "label_instance") ++ attributes.map(_.naiveLabel()))
  }

  /**
   *
   * @param lFutureJsonValue
   * @param attributes
   * @return
   */
  def getValuesFromLazyPage(lFutureJsonValue : LazyFutureJsonValue, attributes: List[URI]) : Future[Map[URI, Map[URI, Literal]]] = {

        lFutureJsonValue.wrapped.map(response => {

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


  def getTypeAttribute(uriEntity : URI, uriAttribute : URI) : Future[URI] = {

    val query = SW(config)
                 .something("instance")
                 .isA(uriEntity)
                 .isSubjectOf(uriAttribute,"vi")
                 .select(List("values"))

    query.map(
      response => {
        response("results")("bindings").arr.map(r => r("vi")("datatype")).distinct match {
          case l if l.length == 1 => URI(l(0).toString())
          case _ => URI("http://www.w3.org/2001/XMLSchema#string")
        }
      })
  }
}
