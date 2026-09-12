package it.unibo.sentinel.boundary.gui.fx.panels

import scalafx.collections.ObservableBuffer
import scalafx.scene.control.{Label, ListView, ScrollPane}
import scalafx.scene.layout.VBox

final case class SideData(title: String, items: Iterable[String])

/** Stable, virtualised lists. Unchanged sections keep their selection and
  * scroll.
  */
final class SidePanel(data: Iterable[SideData]) extends ScrollPane:
  styleClass += "warehouse-sidebar"
  fitToWidth = true
  minWidth = 0
  minHeight = 0
  prefWidth = 240

  private val contentBox = new VBox:
    styleClass += "warehouse-sections"
  private var sections = Vector.empty[Section]
  content = contentBox
  updateData(data)

  def updateData(newData: Iterable[SideData]): Unit =
    val ordered = newData.toVector
    if sections.map(_.title) != ordered.map(_.title) then
      sections = ordered.map(d => new Section(d.title))
      contentBox.children = sections.map(_.content)
    sections.zip(ordered).foreach { (section, data) =>
      section.update(data.items.toVector)
    }

  private final class Section(val title: String):
    private val buffer = ObservableBuffer.empty[String]
    private var items = Vector.empty[String]
    private val list = new ListView[String](buffer):
      styleClass += "warehouse-list"
      fixedCellSize = 28
      minWidth = 0
      prefHeight = 34
      minHeight = 34
      maxHeight = 34

    // A tooltip exposes long mission and route descriptions without making
    // their length dictate the width of the warehouse viewport.
    list.delegate.setCellFactory(_ =>
      new javafx.scene.control.ListCell[String]:
        private val hint = new javafx.scene.control.Tooltip
        setTooltip(hint)
        override protected def updateItem(item: String, empty: Boolean): Unit =
          super.updateItem(item, empty)
          val label = if empty then "" else Option(item).getOrElse("")
          setText(label)
          hint.setText(label)
    )
    val content: VBox = new VBox:
      spacing = 5
      children = Seq(
        new Label(title.toUpperCase):
          styleClass += "warehouse-section-title"
        ,
        list
      )

    def update(updated: Vector[String]): Unit =
      if items != updated then
        val selected = Option(list.delegate.getSelectionModel.getSelectedItem)
        // Replacing only changed rows avoids resetting the virtualised list.
        val common = math.min(items.size, updated.size)
        (0 until common).foreach { index =>
          if items(index) != updated(index) then
            buffer.update(index, updated(index))
        }
        if updated.size < items.size then
          buffer.remove(updated.size, items.size - updated.size)
        else buffer ++= updated.drop(common)
        selected
          .filter(updated.contains)
          .foreach(list.delegate.getSelectionModel.select)
        items = updated
        val height = math.max(1, math.min(5, items.size)) * 28 + 6
        list.prefHeight = height
        list.minHeight = height
        list.maxHeight = height
