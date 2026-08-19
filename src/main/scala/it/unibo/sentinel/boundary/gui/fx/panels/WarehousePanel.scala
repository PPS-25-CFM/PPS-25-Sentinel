package it.unibo.sentinel.boundary.gui.fx.panels

import it.unibo.sentinel.core.warehouse.{Position, Warehouse}
import scalafx.beans.binding.Bindings
import scalafx.geometry.Pos
import scalafx.scene.control.Label
import scalafx.scene.layout.{
  ColumnConstraints,
  GridPane,
  Priority,
  RowConstraints,
  StackPane
}
import scalafx.scene.paint.Color
import it.unibo.sentinel.core.scenario.Spawn
import it.unibo.sentinel.core.robot.value

/** Panel used to display a [[Warehouse]]
  *
  * @param warehouse
  *   the [[Warehouse]] to display
  */
final class WarehousePanel(warehouse: Warehouse, spawns: Seq[Spawn])
    extends GridPane:

  private val rows: Int = warehouse.height
  private val cols: Int = warehouse.width

  alignment = Pos.Center
  hgrow = Priority.Never
  vgrow = Priority.Never

  private val maxCellWidth =
    (width - padding.value.getLeft - padding.value.getRight) / cols
  private val maxCellHeight =
    (height - padding.value.getTop - padding.value.getBottom) / rows
  private val squareCellSize = Bindings.min(maxCellWidth, maxCellHeight)

  columnConstraints = (0 until cols).map { _ =>
    val cc = new ColumnConstraints()
    cc.prefWidth <== squareCellSize
    cc.maxWidth <== squareCellSize
    cc
  }
  rowConstraints = (0 until rows).map { _ =>
    val rc = new RowConstraints()
    rc.prefHeight <== squareCellSize
    rc.maxHeight <== squareCellSize
    rc
  }

  for
    r <- 0 until rows
    c <- 0 until cols
  do
    val traversable = warehouse.isTraversable(Position(c, r))
    val id = spawns.find(_.at == Position(c, r)).map(_.id.value)
    add(createCellNode(traversable, id.getOrElse("")), c, r)

  /** Creates a new cell to add to the grid
    *
    * @param traversable
    *   indicates if the cell is a traversable [[Tile]], used to determine the
    *   cell's color
    * @return
    *   a cell represented by a [[StackPane]]
    */
  private def createCellNode(traversable: Boolean, robotId: String): StackPane =
    val bgColor = if traversable then "#F8FAFC" else "#334155"
    val borderColor = if traversable then "#E0E6ED" else "#1C2739"
    val textColor = if traversable then "#0F172A" else "#F8FAFC"
    val cellLabel = new Label:
      text = robotId
      textFill = Color.web(textColor)
      style = "-fx-font-weight: bold; -fx-font-size: 12px;"

    new StackPane:
      children = Seq(cellLabel)
      minWidth = 0
      minHeight = 0
      style = s"""
        -fx-background-color: $bgColor;
        -fx-border-color: $borderColor;
        -fx-border-width: 1px;
      """
