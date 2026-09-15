package it.unibo.sentinel.boundary.gui.fx.panels

import it.unibo.sentinel.core.warehouse.Position

/** Geometry in logical pixels. Offsets measure scrolling from the top left;
  * smaller warehouses are centred independently on each axis.
  */
private[gui] final case class WarehouseViewport(
    columns: Int,
    rows: Int,
    width: Double = 0,
    height: Double = 0,
    cellSize: Double = 32,
    offsetX: Double = 0,
    offsetY: Double = 0,
    fitting: Boolean = true
):
  def ready: Boolean = width > 0 && height > 0
  def gridWidth: Double = columns.toDouble * cellSize
  def gridHeight: Double = rows.toDouble * cellSize
  def maxOffsetX: Double = math.max(0, gridWidth - width)
  def maxOffsetY: Double = math.max(0, gridHeight - height)
  def originX: Double = math.max(0, (width - gridWidth) / 2) - offsetX
  def originY: Double = math.max(0, (height - gridHeight) / 2) - offsetY
  def x(column: Int): Double = originX + column * cellSize
  def y(row: Int): Double = originY + row * cellSize

  def resize(newWidth: Double, newHeight: Double): WarehouseViewport =
    val resized =
      copy(width = math.max(0, newWidth), height = math.max(0, newHeight))
    if fitting then resized.fit
    else
      resized.scroll(
        offsetX + (width - resized.width) / 2,
        offsetY + (height - resized.height) / 2
      )

  def fit: WarehouseViewport =
    if !ready then copy(fitting = true)
    else
      copy(
        cellSize = math.min(64, math.min(width / columns, height / rows)),
        offsetX = 0,
        offsetY = 0,
        fitting = true
      )

  /** Keeps the world position under the centre of the viewport stationary. */
  def zoom(factor: Double): WarehouseViewport =
    if !ready || factor <= 0 || !factor.isFinite then this
    else
      val minimum = math.min(1, math.min(width / columns, height / rows))
      val size = math.max(minimum, math.min(64, cellSize * factor))
      val centreX = (width / 2 - originX) / cellSize
      val centreY = (height / 2 - originY) / cellSize
      copy(cellSize = size, fitting = false)
        .scroll(centreX * size - width / 2, centreY * size - height / 2)

  def scroll(x: Double, y: Double): WarehouseViewport =
    copy(
      offsetX = math.max(0, math.min(maxOffsetX, x)),
      offsetY = math.max(0, math.min(maxOffsetY, y))
    )

  def positionAt(mouseX: Double, mouseY: Double): Option[Position] =
    val column = math.floor((mouseX - originX) / cellSize).toInt
    val row = math.floor((mouseY - originY) / cellSize).toInt
    Option.when(
      ready && mouseX >= 0 && mouseX < width && mouseY >= 0 && mouseY < height &&
        column >= 0 && column < columns && row >= 0 && row < rows
    )(Position(column, row))

  def visibleColumns: Range = visible(originX, width, columns)
  def visibleRows: Range = visible(originY, height, rows)

  private def visible(origin: Double, extent: Double, count: Int): Range =
    if !ready then 0 until 0
    else
      val start = math.max(0, math.floor(-origin / cellSize).toInt)
      val end = math.min(count, math.ceil((extent - origin) / cellSize).toInt)
      start until end
