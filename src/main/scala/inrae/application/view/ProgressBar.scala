package inrae.application.view

import org.querki.jquery._
import org.scalajs.dom.document

import scalatags.JsDom.all._

import scala.scalajs.js.timers._

object ProgressBar {

  val id_progress_bar = "bar-progress"
  var div_progress_bar = "div-bar-progress"

  $("#processing-data").on("hidden.bs.modal", () => {
    closeWaitModal()
  });

  def setDivProgressBar(percent : Double) = {
   div(
      id:="div-bar-progress",
     `class`:="progress-bar progress-bar-striped progress-bar-animated",
      style:="animation-duration: 500ms;",
      role:="progressbar",
      aria.valuenow:="15",
      aria.valuemin:="0",
      aria.valuemax:="100",
      style:="width: "+Math.round(percent*100.0).toString()+"%"
    )
  }

  var nModal = 0

  def openWaitModal() = {
    this.synchronized {
      nModal +=  1
      if ( nModal == 1 ) {
        $("#message-data-processing").empty().append(p().render)
        $("#processing-data-button").click()
        $("#message-data-processing2").empty().append(p().render)
        $("#processing-data-button2").click()
      }
    }
  }

  def closeWaitModal() = {
    setTimeout(1000) {
      this.synchronized {
        nModal -=  1
        if ( nModal == 0 ) {
            $("#close-processing-data-button").click()
          }
        }
      }
  }

  def modalDisplayError(message: String) = {
      $("#message-data-processing").empty().append(
        div(
          `class`:="alert",
          message
        ).render
      )
  }


  def clean() = {
    $("#"+id_progress_bar).empty().append(setDivProgressBar(0).render)
  }

  def setProgressBar(percent : Double) : Unit = {
    $("#"+id_progress_bar).empty().append(setDivProgressBar(percent).render)

    if (percent >= 1.0) {
      $("#"+div_progress_bar).fadeOut("slow")
      $("#close-processing-data-button").click()
    }
  }

  def setTextProgressBar(text : String) = {
    document.getElementById(div_progress_bar).innerHTML = text
  }
}
