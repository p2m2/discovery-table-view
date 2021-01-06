package inrae.application.view

import inrae.application.TableApp
import inrae.application.discovery.table.util.RequestSemanticDb
import inrae.semantic_web.LazyFutureJsonValue
import inrae.semantic_web.rdf.{Literal, URI}
import org.scalajs.dom.{MouseEvent, document}
import org.scalajs.dom.raw.{HTMLInputElement, HTMLSelectElement, HTMLTableCellElement, HTMLTableSectionElement}
import scalatags.Text.all._
import wvlet.log.Logger.rootLogger.info

import scala.concurrent.Future

case class ValuesTable(requestHandler : RequestSemanticDb) {
  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global

  val id_tr_header_table = "results_table_tr_thead"
  val id_footer_table = "footer_table"
  val id_body_table = "results_table_body"

  var nLazyPages = 0

  def clean() = {
    document.getElementById(id_tr_header_table).innerHTML = tr().render
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

  def updateValues(listLazyPageResults : Seq[LazyFutureJsonValue],
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
    updateFooter(listLazyPageResults,currentPage,lUriAtt.keys.toList.length+1)

  }

  def updateTriggerPages(listFilters : Seq[(URI,String,Literal)] = List()) : Future[Unit] = {
    info(" -- updateValue -- ")
    cleanValues()

    val lUriAtt = currentAttributes()

    requestHandler.getLazyPagesValues(TableApp.currentEntity(), listFilters, lUriAtt.keys.toList ).map((some) => {
      nLazyPages = some._1
      val allLazyPages = some._2
      updateValues(allLazyPages,0)
    })
  }

  def cleanHeader() = document.getElementById(id_tr_header_table).innerHTML = ""

  def updateFooter(listLazyPageResults : Seq[LazyFutureJsonValue],currentPage:Int,colspanValue : Int) = {
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
          updateValues(listLazyPageResults,currentPage-1)
        }
      })

    document.getElementById("pageForwardFooter").addEventListener( "click" ,
      (event:MouseEvent) => {
        if ( currentPage<=nLazyPages) {
          updateValues(listLazyPageResults,currentPage+1)
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
  def updateAttributesHeader() = {
    info(" -- updateAttributesHeader -- ")
    //cleanHeader()

    val uri = TableApp.currentEntity()

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
        ValuesTable(requestHandler).updateTriggerPages()
        FilterTable(requestHandler).updateFilterTable()
    } )
  }

}
