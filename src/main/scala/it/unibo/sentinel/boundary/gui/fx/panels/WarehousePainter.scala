package it.unibo.sentinel.boundary.gui.fx.panels

import it.unibo.sentinel.core.robot.value
import it.unibo.sentinel.core.simulation.RobotSnapshot
import it.unibo.sentinel.core.warehouse.{Area, Position, Tile, Warehouse}
import javafx.scene.canvas.GraphicsContext
import javafx.scene.paint.Color
import javafx.scene.text.{Font, FontWeight, TextAlignment}

/** Drawing only: all coordinates and clipping come from the viewport. */
private[gui] object WarehousePainter:
  private val floor = Color.web("#F8FAFC")
  private val obstacle = Color.web("#334155")
  private val shelf = Color.web("#F59E0B")
  private val bay = Color.web("#86EFAC")
  private val ink = Color.web("#0F172A")
  private val grid = Color.web("#94A3B8", 0.45)
  private val selectionColor = Color.web("#0284C7")
  private val labelFont = Font.font("System", FontWeight.BOLD, 12)
  private val detailFont = Font.font("System", 9)

  def warehouse(
      gc: GraphicsContext,
      view: WarehouseViewport,
      warehouse: Warehouse
  ): Unit =
    gc.clearRect(0, 0, view.width, view.height)
    val columns = view.visibleColumns
    // Merge same-colour cells into horizontal runs. At overview scale a large
    // uniform floor needs one drawing command per row, rather than per cell.
    for row <- view.visibleRows do
      var runStart = columns.start
      var runColor = obstacle
      for column <- columns do
        val color = tileColor(warehouse.tileAt(Position(column, row)))
        if color != runColor then
          fillRun(gc, view, row, runStart, column, runColor)
          runStart = column
          runColor = color
      fillRun(gc, view, row, runStart, columns.end, runColor)

    if view.cellSize >= 8 then
      gc.setStroke(grid)
      gc.setLineWidth(0.5)
      gc.beginPath()
      for column <- columns.start to columns.end do
        gc.moveTo(view.x(column), math.max(0, view.originY))
        gc.lineTo(
          view.x(column),
          math.min(view.height, view.originY + view.gridHeight)
        )
      for row <- view.visibleRows.start to view.visibleRows.end do
        gc.moveTo(math.max(0, view.originX), view.y(row))
        gc.lineTo(
          math.min(view.width, view.originX + view.gridWidth),
          view.y(row)
        )
      gc.stroke()

    if view.cellSize >= 28 then
      gc.setFont(detailFont)
      gc.setFill(ink)
      for
        row <- view.visibleRows
        column <- columns
      do
        val pos = Position(column, row)
        val x = view.x(column)
        val y = view.y(row)
        gc.setTextAlign(TextAlignment.LEFT)
        warehouse.tileAt(pos) match
          case Some(Tile.Shelf(item)) =>
            gc.fillText(item.toString.take(1), x + 3, y + 11)
          case Some(_: Tile.LoadingBay) => gc.fillText("LB", x + 3, y + 11)
          case _                        => ()
        gc.setTextAlign(TextAlignment.RIGHT)
        warehouse
          .traversalCost(pos)
          .foreach(cost =>
            gc.fillText(
              cost.toString,
              x + view.cellSize - 3,
              y + view.cellSize - 3,
              view.cellSize - 6
            )
          )

  def robots(
      gc: GraphicsContext,
      view: WarehouseViewport,
      robots: Seq[RobotSnapshot]
  ): Unit =
    gc.clearRect(0, 0, view.width, view.height)
    gc.setGlobalAlpha(0.35)
    for robot <- robots do
      gc.setFill(robotColor(robot.id.value))
      for
        path <- robot.path
        pos <- path.positions
        if visible(view, pos)
      do gc.fillRect(view.x(pos.x), view.y(pos.y), view.cellSize, view.cellSize)
    gc.setGlobalAlpha(1)
    gc.setFont(labelFont)
    gc.setTextAlign(TextAlignment.CENTER)
    for robot <- robots if visible(view, robot.position) do
      val x = view.x(robot.position.x)
      val y = view.y(robot.position.y)
      gc.setFill(robotColor(robot.id.value))
      gc.fillRect(x, y, view.cellSize, view.cellSize)
      if view.cellSize >= 8 then
        gc.setStroke(ink)
        gc.setLineWidth(1.5)
        gc.strokeRect(
          x + 0.75,
          y + 0.75,
          view.cellSize - 1.5,
          view.cellSize - 1.5
        )
      if view.cellSize >= 20 then
        gc.setFill(ink)
        gc.fillText(
          robot.id.value,
          x + view.cellSize / 2,
          y + view.cellSize / 2 + 4,
          view.cellSize - 4
        )

  def selection(
      gc: GraphicsContext,
      view: WarehouseViewport,
      area: Option[Area]
  ): Unit =
    gc.clearRect(0, 0, view.width, view.height)
    area.foreach { selected =>
      val left = math.max(0, math.min(selected.corner.x, selected.opposite.x))
      val top = math.max(0, math.min(selected.corner.y, selected.opposite.y))
      val right = math.min(
        view.columns,
        math.max(selected.corner.x, selected.opposite.x) + 1
      )
      val bottom = math.min(
        view.rows,
        math.max(selected.corner.y, selected.opposite.y) + 1
      )
      if left < right && top < bottom then
        val width = (right - left) * view.cellSize
        val height = (bottom - top) * view.cellSize
        gc.setFill(selectionColor)
        gc.setGlobalAlpha(0.18)
        gc.fillRect(view.x(left), view.y(top), width, height)
        gc.setGlobalAlpha(1)
        gc.setStroke(selectionColor)
        gc.setLineWidth(math.min(2, view.cellSize))
        val inset = math.min(1, view.cellSize / 2)
        gc.strokeRect(
          view.x(left) + inset,
          view.y(top) + inset,
          width - 2 * inset,
          height - 2 * inset
        )
    }

  private def tileColor(tile: Option[Tile]): Color = tile match
    case Some(_: Tile.Shelf)      => shelf
    case Some(_: Tile.LoadingBay) => bay
    case Some(_: Tile.Walkable)   => floor
    case _                        => obstacle

  private def fillRun(
      gc: GraphicsContext,
      view: WarehouseViewport,
      row: Int,
      start: Int,
      end: Int,
      color: Color
  ): Unit =
    if start < end then
      gc.setFill(color)
      gc.fillRect(
        view.x(start),
        view.y(row),
        (end - start) * view.cellSize,
        view.cellSize
      )

  private def robotColor(id: String): Color =
    Color.hsb((Math.floorMod(id.hashCode, 360) * 137.508) % 360, 0.55, 0.95)

  private def visible(view: WarehouseViewport, pos: Position): Boolean =
    pos.x >= 0 && pos.x < view.columns && pos.y >= 0 && pos.y < view.rows &&
      view.x(pos.x) < view.width && view.x(pos.x) + view.cellSize > 0 &&
      view.y(pos.y) < view.height && view.y(pos.y) + view.cellSize > 0
