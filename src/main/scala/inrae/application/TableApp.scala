package inrae.application

import inrae.application.discovery.table.util.RequestSemanticDb
import inrae.application.view.ValuesTable
import inrae.semantic_web.rdf.URI
import org.querki.jquery.$
import org.scalajs.dom.raw.{HTMLInputElement, HTMLSelectElement}
import org.scalajs.dom.{KeyboardEvent, MouseEvent, document, window}
import scalatags.Text.all._
import wvlet.log.Logger.rootLogger.info

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExportTopLevel

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
      ValuesTable.updateAttributesHeader(requestHandler)
    })
  }

  def setRequestSemanticDb(endpoint:String) = {
    /* new request configuration */
    val requestHandler = RequestSemanticDb(endpoint)

    /* clean tr */
    ValuesTable.clean()

    updateListEntities(requestHandler)

    /* trigger when entity is selected */
    val listEntities = document.getElementById(id_entities_list).asInstanceOf[HTMLSelectElement]

    listEntities.addEventListener( "input" , (_:MouseEvent) => {
        ValuesTable.updateAttributesHeader(requestHandler)
    })
  }

  def updateConfigurationEndpoint(endpointUserOption : Option[String] ): Unit = {

    val endpoint = document.getElementById("endpoint").asInstanceOf[HTMLInputElement]

    $("#button_endpoint").click(() => {
        setRequestSemanticDb(endpoint.value)
      })

    endpoint.addEventListener( "keypress" , (event:KeyboardEvent) => {
      if (event.key == "Enter") {
        setRequestSemanticDb(endpoint.value)
      }
    })

    endpointUserOption match {
      case Some(endpointUser) =>  {
        endpoint.value=endpointUser
        setRequestSemanticDb(endpointUser)
      }
      case None =>
    }

  }

  @JSExportTopLevel("clean_table_view")
  def clean_table_view() = {
    window.location.reload()
  }


  @JSExportTopLevel("sparql_link")
  def sparql_link() = {
    println("sparql_link")
    //    $("#everything").data("requestHandler",requestHandler)
  }

  @JSExportTopLevel("url_link")
  def url_link() = {
    println("url_link")
    //    $("#everything").data("requestHandler",requestHandler)
  }



  def main(args: Array[String]): Unit = {
    println(" -- discovery-table-view -- ")

    /* Get parameters */
    val parameters = (window.location.toString
      .split("\\?")
      .lift(1).map( parameters =>
            Map(
              "endpoint"->"http://endpoint-metabolomics.ara.inrae.fr/peakforest/sparql",
              )
              ++ parameters.split("&")
                      .map(js.URIUtils.decodeURIComponent)
                      .map(paramAndValue => paramAndValue.split("="))
                      .filter(_.length>1)
                      .map( v => v(0) -> v(1)).toMap))


    val endpoint = parameters match {
      case Some(m) => Some(m("endpoint"))
      case _ => None
    }

    updateConfigurationEndpoint(endpoint)
  }
}
