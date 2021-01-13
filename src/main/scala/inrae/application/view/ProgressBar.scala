package inrae.application.view

import org.querki.jquery._
import org.scalajs.dom.document
import scalatags.Text
import scalatags.Text.all._

import scala.scalajs.js.timers._

object ProgressBar {

  val id_progress_bar = "bar-progress"
  var div_progress_bar = "div-bar-progress"


  def setDivProgressBar(percent : Double) :  Text.TypedTag[String] = {
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
        println("OPEN")
        $("#processing-data-button").click()
      }
    }
  }

  def closeWaitModal() = {
    this.synchronized {
      nModal -=  1
      if ( nModal == 0 ) {
        setTimeout(1000) {
          println("CLOSE")
          $("#close-processing-data-button").click()
        }
      }
    }
  }


  def clean() = {
    document.getElementById(id_progress_bar).innerHTML = setDivProgressBar(0).render
  }

  def setProgressBar(percent : Double) : Unit = {
    document.getElementById(id_progress_bar).innerHTML = setDivProgressBar(percent).render

    if (percent >= 1.0) {
      $("#"+div_progress_bar).fadeOut("slow")
      $("#close-processing-data-button").click()
    }
  }

  def setTextProgressBar(text : String) = {
    document.getElementById(div_progress_bar).innerHTML = text
  }
}
