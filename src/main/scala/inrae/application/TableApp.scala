package inrae.application

import inrae.application.discovery.table.util.RequestSemanticDb
import inrae.application.view.{FilterTable, ValuesTable}
import inrae.semantic_web.rdf.URI
import org.scalajs.dom.{MouseEvent, NodeList, document}
import org.scalajs.dom.raw.{HTMLCollection, HTMLDataListElement, HTMLInputElement, HTMLOptionElement, HTMLSelectElement, HTMLTableCellElement, HTMLTableSectionElement}

import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}
import scala.util.{Failure, Success}
import scalatags.Text.all.{list, _}
import sourcecode.Text.generate
import wvlet.log.Logger.rootLogger.{error, info}

object TableApp {
  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global

  val id_entities_list = "entities-list"

  def currentEntity() : URI = {
    info(" -- currentEntity -- ")
    val selectedEntity = document.getElementById(id_entities_list).asInstanceOf[HTMLSelectElement]
    URI(selectedEntity.value)
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
      ValuesTable(requestHandler).updateAttributesHeader()
    })
  }

  def updateConfigurationEndpoint(): Unit = {
    val endpoint = document.getElementById("endpoint").asInstanceOf[HTMLInputElement]

    endpoint.addEventListener( "input" , (event:MouseEvent) => {
      info(" -- new endpoint: "+endpoint.value)

      /* new request configuration */
      val requestHandler = RequestSemanticDb(endpoint.value)

      /* clean tr */
      ValuesTable(requestHandler).clean()

      updateListEntities(requestHandler)

      /* trigger when entity is selected */
      val listEntities = document.getElementById(id_entities_list).asInstanceOf[HTMLSelectElement]
      listEntities.addEventListener( "input" , (event:MouseEvent) => {
        println("updateAttributesHeader")
        ValuesTable(requestHandler).updateAttributesHeader()
      })
    })
  }

  def main(args: Array[String]): Unit = {
    updateConfigurationEndpoint()
  }
}
