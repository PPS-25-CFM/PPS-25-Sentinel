package it.unibo.sentinel.boundary.gui.fx.panels

import scalafx.scene.control.ScrollPane
import scalafx.scene.layout.VBox
import scalafx.geometry.Insets
import scalafx.scene.control.Label
import scalafx.scene.paint.Color
import scalafx.scene.control.ListView
import scalafx.scene.layout.Priority
import scalafx.scene.control.ControlIncludes.jfxMultipleSelectionModel2sfx
import scalafx.scene.control.SelectionMode
import scalafx.collections.ObservableBuffer

case class SideData(
    title: String,
    items: Iterable[String]
)

final class SidePanel(data: Iterable[SideData]) extends ScrollPane:

  style = "-fx-background-color: #0F172A; -fx-background: #0F172A;"
  fitToWidth = true

  private val contentBox = new VBox:
    spacing = 12.0
    padding = Insets(12.0)
    style = "-fx-background-color: #0F172A;"

  private var sections
      : Map[String, (ListView[String], ObservableBuffer[String])] = Map.empty

  content = contentBox
  updateData(data)

  def updateData(newData: Iterable[SideData]): Unit =
    val currentTitles = newData.map(_.title).toSet
    val existingTitles = sections.keySet

    if currentTitles != existingTitles then
      contentBox.children.clear()
      sections = newData.map { d =>
        val (vbox, listView, buffer) = createListSection(d.title, d.items)
        contentBox.children.add(vbox)
        d.title -> (listView, buffer)
      }.toMap
    else
      newData.foreach { d =>
        sections.get(d.title).foreach { case (listView, buffer) =>
          val itemList = d.items.toSeq
          buffer.setAll(itemList*)
          val visibleItems = Math.min(itemList.size, 5).max(1)
          listView.prefHeight = visibleItems * 28 + 6
        }
      }

  /** @param sectionTitle
    *   title of the section
    * @param items
    *   items (text) to display
    * @return
    *   a [[VBox]] containing the given list of items, displaying them
    *   vertically
    */
  private def createListSection(
      sectionTitle: String,
      items: Iterable[String]
  ): (VBox, ListView[String], ObservableBuffer[String]) =
    val itemList = items.toSeq
    val buffer = ObservableBuffer.from(itemList)
    val header = new Label:
      text = sectionTitle.toUpperCase
      textFill = Color.web("#94A3B8")
      style =
        "-fx-font-weight: bold; -fx-font-size: 11px; -fx-letter-spacing: 1px;"

    val visibleItems = Math.min(itemList.size, 5).max(1)
    val calculatedHeight = visibleItems * 28 + 6
    val listView = new ListView[String](buffer):
      prefHeight = calculatedHeight
      maxHeight = 150.0
      vgrow = Priority.Never
      selectionModel().selectionMode = SelectionMode.Single
      style = """
        -fx-background-color: #1E293B;
        -fx-control-inner-background: #1E293B;
        -fx-background-radius: 6px;
        -fx-border-color: #334155;
        -fx-border-radius: 6px;
        -fx-border-width: 1px;
        -fx-cell-size: 28px;
        -fx-font-size: 12px;
        -fx-text-fill: #F8FAFC;
      """

    val sectionBox = new VBox:
      spacing = 4.0
      vgrow = Priority.Never
      children = Seq(header, listView)

    (sectionBox, listView, buffer)
