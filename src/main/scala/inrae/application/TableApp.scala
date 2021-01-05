package inrae.application

import inrae.application.discovery.table.util.RequestSemanticDb
import inrae.application.view.FilterTable
import inrae.semantic_web.rdf.{SparqlBuilder, URI}
import inrae.semantic_web.{LazyFutureJsonValue, SW, StatementConfiguration}
import org.scalajs.dom
import org.scalajs.dom.{MouseEvent, NodeList, document}
import org.scalajs.dom.raw.{HTMLCollection, HTMLDataListElement, HTMLInputElement, HTMLOptionElement, HTMLSelectElement, HTMLTableCellElement, HTMLTableSectionElement}
import scalatags.JsDom.all.bindJsAnyLike

import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}
import scala.util.{Failure, Success}
import scalatags.Text.all.{list, _}
import sourcecode.Text.generate
import wvlet.log.Logger.rootLogger.{error, info}

import scala.concurrent.{Future, Promise}
import scala.scalajs.js
import scala.scalajs.js.JSON

object TableApp {
  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global

  val id_entities_list = "entities-list"
  val id_tr_header_table = "results_table_tr_thead"
  val id_footer_table = "footer_table"
  val id_body_table = "results_table_body"

  var nLazyPages = 0

  def currentEntity() : URI = {
    info(" -- currentEntity -- ")
    val selectedEntity = document.getElementById(id_entities_list).asInstanceOf[HTMLSelectElement]
    URI(selectedEntity.value)
  }

  def currentAttributes() : Map[URI,Int] = {
    info(" -- currentAttributes -- ")
    val th = document.getElementById(id_tr_header_table).getElementsByTagName("th")

    (0 to th.length-1).map( i => {
      val elt : HTMLTableCellElement = th(i).asInstanceOf[HTMLTableCellElement]

      val inputList = elt.getElementsByTagName("input")
      if (inputList.length>0) {
        try {
          val inp: HTMLInputElement = inputList(0).asInstanceOf[HTMLInputElement]
          Some(URI(elt.id) -> inp.value.toInt)
        } catch {
          case _ : Throwable => None
        }
      } else {
        None
      }
    }).toList.flatten.toMap
  }

  def cleanValues() = document.getElementById(id_body_table).asInstanceOf[HTMLTableSectionElement].innerHTML = ""

  def updateValues(requestHandler : RequestSemanticDb,
                   listLazyPageResults : Seq[LazyFutureJsonValue],
                   currentPage: Int) : Unit = {

    val tbody = document.getElementById(id_body_table).asInstanceOf[HTMLTableSectionElement]

    /* add wait logo ?*/
    tbody.innerHTML = ""

    val lUriAtt = currentAttributes()

    requestHandler.getValuesFromLazyPage(listLazyPageResults(currentPage), lUriAtt.keys.toList).map( lValues => {
      tbody.innerHTML = lValues.keys.map(  uriInstance => {

        val mapUriAndLiteral = lValues(uriInstance)

        val labelUriInstance = mapUriAndLiteral.get(uriInstance) match {
          case Some(label) => label.value
          case None => uriInstance.naiveLabel()
        }

        val e = lUriAtt.keys.map( kUriAtt => {
          lUriAtt(kUriAtt) -> td(mapUriAndLiteral(kUriAtt).naiveLabel())
        }).toList.sortWith( _._1 < _._1).map( _._2)
        tr(td(a(href:=uriInstance.localName,target:="_blank",labelUriInstance)),e)
      }).map( _.render ).mkString("")
    })

    /* footer update */
    updateFooter(requestHandler,listLazyPageResults,currentPage,lUriAtt.keys.toList.length+1)

  }

  def updateTriggerPages(requestHandler : RequestSemanticDb) = {
    info(" -- updateValue -- ")
    cleanValues()

    val lUriAtt = currentAttributes()

    requestHandler.getLazyPagesValues(currentEntity(), lUriAtt.keys.toList ).map((some) => {
      nLazyPages = some._1
        val allLazyPages = some._2
        updateValues(requestHandler,allLazyPages,0)
    })
  }

  def cleanHeader() = document.getElementById(id_tr_header_table).innerHTML = ""

  def updateFooter(requestHandler : RequestSemanticDb, listLazyPageResults : Seq[LazyFutureJsonValue],currentPage:Int,colspanValue : Int) = {
    info(" -- updateFooter -- ")
    val footer = document.getElementById(id_footer_table)

    footer.innerHTML = tr(
      td(
        colspan:=colspanValue, // name instance and attributes
          `class`:="link",
              span(id:="pageBackFooter",
               a(href:="#",
                raw("&laquo;"))),
                currentPage.toString + "/" + nLazyPages.toString , //id:="page"+x.toString(),
              span(
                id:="pageForwardFooter",
                a(href:="#",raw("&raquo;"))
              )
        )
      ).render

    document.getElementById("pageBackFooter").addEventListener( "click" ,
      (event:MouseEvent) => {
        if ( currentPage>0) {
          updateValues(requestHandler,listLazyPageResults,currentPage-1)
        }
      })

    document.getElementById("pageForwardFooter").addEventListener( "click" ,
      (event:MouseEvent) => {
        if ( currentPage<=nLazyPages) {
          updateValues(requestHandler,listLazyPageResults,currentPage+1)
        }
      })

  }


  /**
   * Build theader tag . Contains
   *  - Header Entity + Attribute List Label  (th tag)
   *  - input hidden to save column order
   *
   * @param requestHandler
   * @return
   */
  def updateAttributesHeader(requestHandler : RequestSemanticDb) = {
    info(" -- updateAttributesHeader -- ")
    //cleanHeader()

    val uri = currentEntity()

    /* set new header */
    requestHandler.getAttributes(uri).map( attributes =>  {

      attributes.zipWithIndex.map( objet => {
        val attrib = objet._1
        val idx = objet._2
        println(attrib+","+idx)
        th(id:=attrib._1.toString, // uri
          attrib._2,              // label
          input(`type`:="hidden" , value:=idx+1))} ) // rang
    }  ).map (
      list => {
        document.getElementById(id_tr_header_table).innerHTML = tr(th(" - "),list).render
      }).andThen ( _ => {
      FilterTable(requestHandler).updateFilterTable()
      updateTriggerPages(requestHandler)
    } )
  }

  def updateListEntities(requestHandler : RequestSemanticDb) = {
    info(" -- updateListEntities -- ")
    document.getElementById(id_entities_list).innerHTML = ""
    requestHandler.getEntities().map ( list => {
      info(" -- entities --")
      document.getElementById(id_entities_list).innerHTML = list.map( uriAndLabel => {
        option(
          value:=uriAndLabel._1.toString(),uriAndLabel._2
        )
      }).render
      updateAttributesHeader(requestHandler)
    })
  }

  def updateConfigurationEndpoint(): Unit = {
    val endpoint = document.getElementById("endpoint").asInstanceOf[HTMLInputElement]

    endpoint.addEventListener( "input" , (event:MouseEvent) => {
      info(" -- new endpoint: "+endpoint.value)
      /* clean tr */
      document.getElementById(id_tr_header_table).innerHTML = tr().render
      /* new request configuration */
      val requestHandler = RequestSemanticDb(endpoint.value)

      updateListEntities(requestHandler)

      /* trigger when entity is selected */
      val listEntities = document.getElementById(id_entities_list).asInstanceOf[HTMLSelectElement]
      listEntities.addEventListener( "input" , (event:MouseEvent) => {
        updateAttributesHeader(requestHandler)
      })
    })
  }

  def snippet(attributes : List[String]) = {

  }

  def main(args: Array[String]): Unit = {

    /*
    val options = document.getElementById("endpoints")
      .asInstanceOf[HTMLDataListElement]
      .options.asInstanceOf[js.Array[HTMLOptionElement]]

    println("Options.............")
    println(options.map(x => println(x.innerText)))



    options.foreach(option => option.addEventListener( "input" , (event:MouseEvent) => {
      println("HOura2:" + option.outerHTML)
    }))
*/

    updateConfigurationEndpoint()

    //println("JSON1->"+JSON.stringify(options))

/*
    datalist.options.as addEventListener(
      "input", (event : MouseEvent) => {
        println("Hello Input:")
        println(options.nodeValue)
        document.getElementById("endpoint_selected").innerHTML = options.nodeValue
      }
    )*/

    /*
    RequestSemanticDb(endpoint,method).getAttributes().map( attrib =>  th(attrib) ) onComplete {
      case Success(list) => document.getElementById("tr_thead").innerHTML = tr(list).render
      case Failure(_) => println("Erreur") // boostrap Affichage erreur ?
    }

*/
//document.body.innerHTML  =
/*
    println("start----------------------------------");
    val config: StatementConfiguration = new StatementConfiguration()
    config.setConfigString(
      """
        |{
        | "sources" : [{
        |   "id"  : "dbpedia",
        |   "url" : "https://dbpedia.org/sparql",
        |   "typ" : "tps",
        |   "method" : "POST"
        | }]}
        |""".stripMargin)

    val query = new SW(config)
    val r = query.something("h1")
      .set(URI("http://dbpedia.org/resource/%C3%84lvdalen"))
      .isSubjectOf(URI("http://www.w3.org/2002/07/owl#sameAs"))
      .select()

    r onComplete {
      case Success(response) => appendPar(document.body, response.toString)
      case Failure(e) => appendPar(document.body, e.getMessage())
    }

   // r.then( (response => println(response.toString)), (error => println(error)) )
     //.catch(failure => appendPar(document.body, failure.toString))

    appendPar(document.body, "Hello World")
 */
  }
}
