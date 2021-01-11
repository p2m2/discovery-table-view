package inrae.application.view

import org.querki.jquery._
import org.scalajs.dom.document
import scalatags.Text
import scalatags.Text.all._

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

  def clean() = {
    document.getElementById(id_progress_bar).innerHTML = setDivProgressBar(0).render
  }

  def setProgressBar(percent : Double) : Unit = {
    document.getElementById(id_progress_bar).innerHTML = setDivProgressBar(percent).render
    $(div_progress_bar).fadeOut()

  }

  def setTextProgressBar(text : String) = {
    document.getElementById(div_progress_bar).innerHTML = text
  }
}
