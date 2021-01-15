package inrae.application.view

import inrae.application.TableApp
import inrae.application.discovery.table.util.RequestSemanticDb
import inrae.semantic_web.SWTransaction
import inrae.semantic_web.rdf.{Literal, URI}
import org.scalajs.dom.document
import org.scalajs.dom.raw.{HTMLInputElement, HTMLTableCellElement, HTMLTableSectionElement}
import scalatags.Text.all._
import wvlet.log.Logger.rootLogger.info

import scala.concurrent.Future

case object ValuesTable {
  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global

  val id_tr_header_table = "results_table_tr_thead"
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

  def updateValues(requestHandler : RequestSemanticDb,
                   listLazyPageResults : Seq[SWTransaction],
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

    PageSelector.update(requestHandler,listLazyPageResults,currentPage)
  }

  def updateTriggerPages(requestHandler : RequestSemanticDb,listFilters : Seq[(URI,String,Literal)] = List()) : Future[Unit] = {
    info(" -- updateValue -- ")
    cleanValues()

    val lUriAtt = currentAttributes()

    requestHandler.getLazyPagesValues(TableApp.currentEntity(), listFilters, lUriAtt.keys.toList ).map((some) => {
      nLazyPages = some._1
      val allLazyPages = some._2
      updateValues(requestHandler,allLazyPages,0)
    })
  }

  def cleanHeader() = document.getElementById(id_tr_header_table).innerHTML = ""

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

    val uri = TableApp.currentEntity()

    /* set new header */
    requestHandler.getAttributes(uri).map( attributes =>  {

      attributes.zipWithIndex.map( objet => {
        val attrib = objet._1
        val idx = objet._2
        th(id:=attrib._1.toString, // uri
          attrib._2,              // label
          input(`type`:="hidden" , value:=idx+1))} ) // rang
    }  ).map (
      list => {
        document.getElementById(id_tr_header_table).innerHTML = tr(th(" - "),list).render
      }).andThen ( _ => {
        ValuesTable.updateTriggerPages(requestHandler)
        FilterTable.updateFilterTable(requestHandler)
    } )
  }

}
