package inrae.application.view

import inrae.application.discovery.table.util.RequestSemanticDb
import inrae.semantic_web.SWTransaction
import org.querki.jquery.$
import org.scalajs.dom.MouseEvent
import scalatags.JsDom.all._
import scalatags.JsDom.tags2.nav

object PageSelector {

  val pageSelectorContainer = "pageSelector_container"

  val nbPagesSelectorContainer = 10

  def update(requestHandler : RequestSemanticDb,listLazyPageResults : Seq[SWTransaction],currentPage:Int) = {

    val minPage = currentPage - nbPagesSelectorContainer match {
      case v if v>=0 => v
      case _ => 0
    }
    val lastPage = currentPage + nbPagesSelectorContainer match {
      case v if v<listLazyPageResults.length => v
      case _ => listLazyPageResults.length-1
    }

    $("#"+pageSelectorContainer).empty().append(
      nav(
        aria.label:="Page navigation discovery-table-view",
        ul(
          `class`:="pagination",
          li(
            `class`:="page-item",
            a(
              `class`:="page-link",
              href:="#",
              onclick:={
                (e: MouseEvent) =>
                  ValuesTable.updateValues(requestHandler,listLazyPageResults,minPage)
              },
              aria.label:="Previous",
              span(
                aria.hidden:="true",
                raw("&laquo;")
              ),
              span(
                `class`:="sr-only",
                "Previous"
              )
            )
          ),
          (currentPage to lastPage).toArray.map( idx => {
              li(
                `class`:="page-item",
                a(
                  `class`:="page-link",
                   href:="#",
                   onclick:=( (event: MouseEvent) => {
                     ValuesTable.updateValues(requestHandler,listLazyPageResults,idx)
                   }),
                  idx.toString,
                )
              )
            }
          ),
          li(
            `class`:="page-item",
            a(
              `class`:="page-link",
              href:="#",
              onclick:={
                (e: MouseEvent) =>
                  ValuesTable.updateValues(requestHandler,listLazyPageResults,lastPage)
              },
              aria.label:="Nex",
              span(
                aria.hidden:="true",
                raw("&raquo;")
              ),
              span(
                `class`:="sr-only",
                "Next"
              )
            )
          )
        )
      ).render)
  }

}
