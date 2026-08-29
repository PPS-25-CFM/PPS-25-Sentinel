package it.unibo.sentinel.boundary.gui.fx.panels

import it.unibo.sentinel.core.warehouse.{Position, Warehouse}
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
import it.unibo.sentinel.core.robot.value
import it.unibo.sentinel.core.simulation.RobotSnapshot

/** Panel used to display a [[Warehouse]]
  *
  * @param warehouse
  *   the [[Warehouse]] to display
  */
final class WarehousePanel(warehouse: Warehouse) extends GridPane:

  private val rows: Int = warehouse.height
  private val cols: Int = warehouse.width

  alignment = Pos.Center
  hgrow = Priority.Always
  vgrow = Priority.Always

  private val cellLabels: Map[Position, Label] = (
    for
      r <- 0 until rows
      c <- 0 until cols
      pos = Position(c, r)
    yield
      val (node, label) = createCellNode(warehouse.isTraversable(pos))
      add(node, c, r)
      pos -> label
  ).toMap

  columnConstraints = (0 until cols).map { _ =>
    new ColumnConstraints:
      hgrow = Priority.Always
      percentWidth = 100.0 / cols
  }
  rowConstraints = (0 until rows).map { _ =>
    new RowConstraints:
      vgrow = Priority.Always
      percentHeight = 100.0 / rows
  }

  /** Updates the robots in the grid
    * @param robots
    *   the new (updated) robots
    */
  def updateRobots(robots: Seq[RobotSnapshot]): Unit =
    cellLabels.values.foreach(_.text = "")
    for robot <- robots do
      cellLabels.get(robot.position).foreach(_.text = robot.id.value)

  /** @param traversable
    *   true if the cell is traversable, false otherwise
    * @return
    *   a tuple containing a `StackPane` (cell) and a label (text inside the
    *   cell)
    */
  private def createCellNode(traversable: Boolean): (StackPane, Label) =
    val bgColor = if traversable then "#F8FAFC" else "#334155"
    val borderColor = if traversable then "#E0E6ED" else "#1C2739"
    val textColor = if traversable then "#0F172A" else "#F8FAFC"

    val label = new Label:
      textFill = Color.web(textColor)
      style = "-fx-font-weight: bold; -fx-font-size: 12px;"

    val pane = new StackPane:
      children = Seq(label)
      style = s"""
        -fx-background-color: $bgColor;
        -fx-border-color: $borderColor;
        -fx-border-width: 1px;
      """
    (pane, label)
