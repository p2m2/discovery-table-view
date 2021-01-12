package inrae.application.view

import inrae.application.TableApp
import inrae.application.discovery.table.util.RequestSemanticDb
import inrae.semantic_web.rdf.{Literal, URI}
import org.scalajs.dom.raw.{HTMLInputElement, HTMLSelectElement, HTMLTableSectionElement}
import org.scalajs.dom.{MouseEvent, document}
import scalatags.Text
import scalatags.Text.all._
import wvlet.log.Logger.rootLogger.info

case class FilterTable(requestHandler : RequestSemanticDb) {
  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global

  val prefix_box = "box"
  val id_filter_table_body = "filter_container"
  val _button_add_filter="add_filter"
  val _button_apply_filter="apply_filter"
  var l_box_filter : List[(URI,URI)] = List()

  def button_add_filter() : Text.TypedTag[String] = {
    button(id:=_button_add_filter, `class`:="btn btn-sm btn-primary", width:="120px", "Add filter")
  }

  def button_apply() : Text.TypedTag[String] = {
    button(id:=_button_apply_filter, `class`:="btn btn-sm btn-primary", width:="120px", "Apply")
  }

  def button_apply_action(): Unit = {
    document.getElementById(_button_apply_filter).addEventListener(
      "click" ,
      (event:MouseEvent) => {
        val listFilter = l_box_filter.map(_._2).zipWithIndex.flatMap(v => {
          val uri = v._1
          val idx = v._2
          val typeBox = l_box_filter(idx)._1


          typeBox.toString() match {
            case "<http://www.w3.org/2001/XMLSchema#integer>" | "<http://www.w3.org/2001/XMLSchema#float>" |
                 "<http://www.w3.org/2001/XMLSchema#double>" |
                 "<http://www.w3.org/2001/XMLSchema#decimal>" => {

              val bufVal = document.getElementById(prefix_box + idx + "_operand").asInstanceOf[HTMLInputElement].value
              if ( bufVal.trim() != "") {
                val operand = Literal(bufVal, typeBox)
                val operator = document.getElementById(prefix_box + idx + "_operator").asInstanceOf[HTMLSelectElement].value
                Option(uri, operator , operand)
              }
              else
                None
            }
            case "<http://www.w3.org/2001/XMLSchema#boolean>" => {
              val boolValue = document.getElementById(prefix_box + idx +"_bool").asInstanceOf[HTMLSelectElement].value
              Option(uri,"=",Literal(boolValue,URI("http://www.w3.org/2001/XMLSchema#boolean")))
            }
            case _ => {
              document.getElementById(prefix_box + idx + "_search") match {
                case v if v != null => {
                  val value = v.asInstanceOf[HTMLInputElement].value
                  if (value.trim() != "")
                    Option(uri, "contains", Literal(value))
                  else
                    None
                }
            }
          }
         }
        })

        /* update triggered page with current filters */
        ValuesTable(requestHandler).updateTriggerPages(listFilter)
      })
  }

  def updateFilterTable() : Unit = {
    info(" -- updateFilterTable -- ")

    /* Print Button and Filter Box */
    val body = document.getElementById(id_filter_table_body).asInstanceOf[HTMLTableSectionElement]

    /* button add + box filter */
    body.innerHTML =
      div(
        `class`:="row row-cols-auto",
        div(
          `class`:="col",
          button_add_filter()
        ),
        div(
          `class`:="col",
          button_apply()
        ) , l_box_filter.zipWithIndex.map( GroupAndIdx  => {
        val typeAndAttributeUri = GroupAndIdx._1
        val idx = GroupAndIdx._2
          div(
            `class`:="col", filter_box(idx,typeAndAttributeUri._1,typeAndAttributeUri._2)
          )
      })).render

    /* set up trigger to remove box filter */
    l_box_filter.zipWithIndex.map(_._2).foreach( index => {
        document.getElementById(prefix_box+index.toString).addEventListener("click" , (event:MouseEvent) => {
          l_box_filter = l_box_filter.take(index) ++ l_box_filter.drop(index+1)
          updateFilterTable()
        })
      })

    /* Execute request when "apply" is triggered */
    button_apply_action()

    val entityClassUri  : URI     = TableApp.currentEntity()
    val attributes : Map[URI,Int] = ValuesTable(requestHandler).currentAttributes()

    val add_f = document.getElementById(_button_add_filter)

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
              l_box_filter = l_box_filter ++ List((`type`,attributePropertyUri))
              /* refresh list box */
              updateFilterTable()

              document.getElementById(_button_add_filter)
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

    val idBoxString = prefix_box+idBox.toString
    val title = attributePropertyUri.naiveLabel()

    `type`.toString() match {
      case "<http://www.w3.org/2001/XMLSchema#integer>" | "<http://www.w3.org/2001/XMLSchema#float>" |
           "<http://www.w3.org/2001/XMLSchema#double>" |
           "<http://www.w3.org/2001/XMLSchema#decimal>" => real_filter_box(idBoxString, title)
      case "<http://www.w3.org/2001/XMLSchema#boolean>" => boolean_filter_box(idBoxString, title)
      case _ => string_filter_box(idBoxString, title)
    }
  }

  def string_filter_box(idBox: String, title : String) : Text.TypedTag[String] = {
    div(
      `class`:="container",
      div(
        `class`:="row",
        div(
          `class`:="col",
          title
        ),
        div(
          `class`:="col",
          i(id:=idBox,`class`:="fas fa-times" )
        )
      ),
      div(
        `class`:="row",
        input(
          id := idBox+"_search",
          `type` := "search")
      )
    )
  }

  def real_filter_box(idBox: String,title : String) : Text.TypedTag[String] = {
    div(
      `class`:="container",
      div(
        `class`:="row",
        div(
          `class`:="col",
          title
        ),
        div(
          `class`:="col",
          i(id:=idBox,`class`:="fas fa-times" )
        )
      ),
      div(
        `class`:="row",
        div(
          `class`:="col",
          select(
            id := idBox+"_operator",
            name := "operator",
            option(value:="=","=",selected),
            option(value:="<","<"),
            option(value:="<=","<="),
            option(value:=">",">"),
            option(value:=">=",">="),
            option(value:="<>","<>"),
          )
        ),
        div(
          `class`:="col",
          input(id := idBox+"_operand",size:="10",`type` := "text"))
      )
    )
  }

  def boolean_filter_box(idBox: String, title : String) : Text.TypedTag[String] = {
    div(
      `class`:="container",
      div(
        `class`:="row",
        div(
          `class`:="col",
          title
        ),
        div(
          `class`:="col",
          i(id:=idBox,`class`:="fas fa-times" )
        )
      ),
      div(
        `class`:="row",
        div(
          `class`:="col",
          select(
            id := idBox+"_bool",
            name := "bool_value",
            option(value:="true","true",selected),
            option(value:="false","false")
          )
        )
      )
    )
  }

}
