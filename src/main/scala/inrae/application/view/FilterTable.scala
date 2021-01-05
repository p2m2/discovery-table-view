package inrae.application.view

import inrae.application.TableApp
import inrae.application.discovery.table.util.RequestSemanticDb
import inrae.semantic_web.rdf.URI
import org.scalajs.dom.{MouseEvent, document}
import org.scalajs.dom.raw.{HTMLSelectElement, HTMLTableSectionElement}
import scalatags.JsDom.all.bindJsAnyLike
import scalatags.Text
import scalatags.Text.all._
import wvlet.log.Logger.rootLogger.info

case class FilterTable(val requestHandler : RequestSemanticDb) {
  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global

  val id_filter_table_body = "filter_table_body"

  var l_box_filter : List[(URI,URI)] = List()

  def button_add_filter() = {
    button(id:="add_filter", width:="100%","Add filter")
  }

  def button_apply() = {
    button(id:="apply_filter", width:="100%","Apply")
  }

  def updateFilterTable() : Unit = {
    info(" -- updateFilterTable -- ")

    val body = document.getElementById(id_filter_table_body).asInstanceOf[HTMLTableSectionElement]
    println("SIZE BOX ="+l_box_filter.length)
    /* button add + box filter */
    body.innerHTML =
      tr(td(button_add_filter(),button_apply()) ,l_box_filter.zipWithIndex.map( GroupAndIdx  => {
        val typeAndAttributeUri = GroupAndIdx._1
        val idx = GroupAndIdx._2
        td(
          filter_box(idx,typeAndAttributeUri._1,typeAndAttributeUri._2)
        )
      })).render

    l_box_filter.zipWithIndex.map(_._2).foreach( index => {
        document.getElementById("box"+index.toString).addEventListener("click" , (event:MouseEvent) => {
          println("UPDATE"+index)
          l_box_filter = l_box_filter.take(index) ++ l_box_filter.drop(index+1)
          updateFilterTable()
        })
      })

    val entityClassUri  : URI     = TableApp.currentEntity()
    val attributes : Map[URI,Int] = TableApp.currentAttributes()

    val add_f = document.getElementById("add_filter")

    /**
     * When clicking on button "Add filter" => display Attribute List to select to add a new filter box
     */
    add_f.addEventListener( "click" ,
      (event:MouseEvent) => {
        add_f.outerHTML = select(
          id:="select_filter",
          attributes.keys.map( uri => {
          option(
            value:=uri.toString(),uri.naiveLabel()
          )
        }).toList).render

        val select_f = document.getElementById("select_filter").asInstanceOf[HTMLSelectElement]

        select_f.addEventListener( "click" ,
          (event:MouseEvent) => {
            val attributePropertyUri = URI(select_f.value)
            requestHandler.getTypeAttribute(entityClassUri,attributePropertyUri).map( `type`  => {
              println("type===>"+`type`)
              l_box_filter = l_box_filter ++ List((`type`,attributePropertyUri))
              /* refresh list box */
              updateFilterTable()

              document.getElementById("add_filter")
                .addEventListener("click" , (event:MouseEvent) => { updateFilterTable() })
            })
          })

        select_f.addEventListener( "blur" ,
          (event:MouseEvent) => {
            updateFilterTable()
          })
      })
  }

  def filter_box(idBox: Int, `type` : URI, attributePropertyUri : URI) : Text.TypedTag[String] = {

    val idBoxString = "box"+idBox.toString
    val title = attributePropertyUri.naiveLabel()

    `type`.toString() match {
      case "<http://www.w3.org/2001/XMLSchema#integer>" | "<http://www.w3.org/2001/XMLSchema#float>" |
           "<http://www.w3.org/2001/XMLSchema#double>" |
           "<http://www.w3.org/2001/XMLSchema#decimal>" => real_filter_box(idBoxString, title, attributePropertyUri)
      case _ => string_filter_box("box"+idBox.toString, title, attributePropertyUri)
    }
  }

  def string_filter_box(idBox: String, title : String, attributePropertyUri : URI) : Text.TypedTag[String] = {
    table(
      tr(
        td(title+" ", i(id:=idBox,`class`:="fas fa-times" ))
      ),
      tr(
        td(input(`type` := "search"))
      )
    )
  }

  def real_filter_box(idBox: String,title : String, attributePropertyUri : URI) : Text.TypedTag[String] = {
    table(
      tr(
        td(title+" ", i(id:=idBox,`class`:="fas fa-times" ))
      ),
      tr(
        td(input(size:="10", `type` := "text")),
        td(
          select(
            name := "operator",
            option(value:="=","=",selected),
            option(value:="<","<"),
            option(value:="<=","<="),
            option(value:=">",">"),
            option(value:=">=",">="),
            option(value:="<>","<>"),
          )
        ),
        td(input(size:="10",`type` := "text"))
      )
    )
  }

}
