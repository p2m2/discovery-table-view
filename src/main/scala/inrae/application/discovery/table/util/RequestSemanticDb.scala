package inrae.application.discovery.table.util

import inrae.application.view.ProgressBar
import inrae.semantic_web.rdf.{Literal, QueryVariable, SparqlBuilder, URI}
import inrae.semantic_web.{SWDiscovery, SWTransaction, StatementConfiguration}
import wvlet.log.Logger.rootLogger.{error, info}

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

case class RequestSemanticDb(endpoint: String, method: String = "POST", `type`: String = "tps") {

  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global

  val config: StatementConfiguration = StatementConfiguration.setConfigString(
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
         "driver" : "inrae.semantic_web.driver.SHTTPDriver",
         "pageSize" : 20
       } }
      """.stripMargin)

  type Results = Map[URI, Map[URI, Literal]]
  var cacheLazyPageResults =  Map[SWTransaction,Map[URI, Map[URI, Literal]]]()

  def manageRequestProgression(transaction : SWTransaction ) = {
    /* progress bar management */
    transaction.progression( (percent) => { ProgressBar.setProgressBar(percent)})
    transaction.requestEvent( (step) => {
      if ( step == "START") {
        ProgressBar.openWaitModal()
      }
      ProgressBar.setTextProgressBar(step)
      if (step == "REQUEST_DONE") {
        ProgressBar.closeWaitModal()
      }
    })
  }


  def getEntities(): Future[List[(URI, String)]] = {

    val transaction =
      SWDiscovery(config).something("instance")
      .isSubjectOf(URI("a"))
        .set(URI("Class", "owl"))
      .focus("instance")
        .isSubjectOf(QueryVariable("attribute"))
          .filter.isLiteral  /* at least one literal */
      .focus("instance")
          .filter.not.contains("http://www.w3.org/2002/07/owl")
      .focus("instance")
        .datatype(URI("label", "rdfs"), "label")
      .select(List("instance", "label"))

    manageRequestProgression(transaction)

    transaction.commit().raw
      .map(response => { response("results")("bindings").arr.map(r => {

        val uri = SparqlBuilder.createUri(r("instance"))

        try {
          (uri, SparqlBuilder.createLiteral(response("results")("datatypes")("label")(uri.toString)(0)).toString)
        } catch {
          case _: Throwable => (uri, uri.naiveLabel())
        }
      }).toList })
      .recover( v => {
        ProgressBar.modalDisplayError(v.getMessage())
        List()
      } )
  }

  def getAttributes(selectedEntity: URI): Future[List[(URI, String)]] = {
    info(" -- getAttributes --")
    info("uri:" + selectedEntity.toString())
    val transaction = SWDiscovery(config).something("attributeProperty")
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

    manageRequestProgression(transaction)

    transaction.commit().raw.map(
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

  def getLazyPagesValues(entity: URI, listFilters : Seq[(URI,String,Literal)], attributes: List[URI]): Future[(Int,Seq[SWTransaction])]= {
    info(" -- getValues --")

    /* empty cache */
    cacheLazyPageResults =  Map[SWTransaction,Map[URI, Map[URI, Literal]]]()

    var query = SWDiscovery(config).something("instance")
      .isA(entity)
//
    /* if an attribute filter is defined with add an attribute on the query otherwise is better to defined a dataset to get missing data */
    attributes.filter( listFilters.map( _._1 ).contains(_)  ).foreach(attribute => {
      query = query.focus("instance").isSubjectOf(attribute)
      /* add filter contains */
      listFilters.filter( _._1 == attribute ) foreach {
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

    attributes.foreach(attribute => {
      query = query.focus("instance").datatype(attribute, attribute.naiveLabel())
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
  def getValuesFromLazyPage(lFutureJsonValue : SWTransaction, attributes: List[URI]) : Future[Map[URI, Map[URI, Literal]]] = {

    cacheLazyPageResults.get(lFutureJsonValue) match {
      case Some(v) => Future { v }
      case None => {
        val transaction = lFutureJsonValue
        this.synchronized {
          manageRequestProgression(transaction)
          transaction.commit().raw.map(response => {
            val res = response("results")("bindings").arr.map(row => {
              val uriInstance = SparqlBuilder.createUri(row("instance"))
              (uriInstance -> (attributes.map(
                uri => {
                  val lit = response("results")("datatypes")(uri.naiveLabel()).obj.getOrElse(uriInstance.localName,ujson.Arr()).arr.headOption match {
                    case Some(value) => SparqlBuilder.createLiteral(value) //SparqlBuilder.createLiteral(row(uri.naiveLabel()))
                    case None => Literal("--")
                  }
                  (uri -> lit)
                }
              ) ++ {
                val labelInstance = SparqlBuilder.createLiteral(response("results")("datatypes")("label_instance")(uriInstance.localName).arr.head)
                List(uriInstance -> labelInstance)
              }).toMap)
            }).toMap
            cacheLazyPageResults = cacheLazyPageResults ++ Map(lFutureJsonValue -> res)
            res
          })
        }
      }
    }
  }

  /*
      Retourn datatype and some values to fill an example list
   */
  def getTypeAttribute(uriEntity : URI, uriAttribute : URI) : Future[(URI,Seq[String])] = {

    val transaction = SWDiscovery(config)
                 .something("instance")
                 .isA(uriEntity)
                 .isSubjectOf(uriAttribute,"vi")
                 .select(List("values"),8)

    manageRequestProgression(transaction)

    transaction.commit().raw.map(
      response => {
        println("test..")

        val values = response("results")("bindings").arr.map(r => SparqlBuilder.createLiteral(r("vi")).value).distinct.toSeq
        println(values)
        Try(response("results")("bindings").arr.map(r => r("vi")("datatype")).distinct match {
          case l if l.length == 1 => URI(l(0).toString())
          case _ => URI("http://www.w3.org/2001/XMLSchema#string")
        }) match {
          case Success(v) => (v,values)
          case Failure(_) => (URI("http://www.w3.org/2001/XMLSchema#string"),values)
        }
      })
  }
}
