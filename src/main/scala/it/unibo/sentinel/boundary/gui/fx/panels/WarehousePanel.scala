package it.unibo.sentinel.boundary.gui.fx.panels

import it.unibo.sentinel.core.simulation.RobotSnapshot
import it.unibo.sentinel.core.warehouse.{Area, Position, Warehouse}
import javafx.application.Platform
import scalafx.geometry.Orientation
import scalafx.scene.canvas.Canvas
import scalafx.scene.control.ScrollBar
import scalafx.scene.input.MouseEvent
import scalafx.scene.layout.{BorderPane, Pane}

/** Shared warehouse viewport. Canvas layers have the size of the visible area,
  * regardless of the warehouse dimensions or zoom level.
  */
final class WarehousePanel(initial: Warehouse) extends BorderPane:
  private var warehouse = initial
  private var robots = Seq.empty[RobotSnapshot]
  private var selection = Option.empty[Area]
  private var geometry = WarehouseViewport(initial.width, initial.height)
  private val tilesLayer = new Canvas
  private val robotsLayer = new Canvas
  private val selectionLayer = new Canvas
  private val layers = Seq(tilesLayer, robotsLayer, selectionLayer)
  private val viewport = new Pane:
    minWidth = 0
    minHeight = 0
    prefWidth = 640
    prefHeight = 480
    styleClass += "warehouse-viewport"
    children = layers
  private val horizontal = new ScrollBar
  private val vertical = new ScrollBar:
    orientation = Orientation.Vertical
  private var syncingScrollbars = false
  private var redrawPending = false
  private var tilesDirty = true
  private var robotsDirty = true
  private var selectionDirty = true

  /** Cell interactions are inert until an editor attaches its callbacks. */
  var onPrimaryClick: Position => Unit = _ => ()
  var onShiftClick: Position => Unit = _ => ()
  var onSecondaryClick: (Position, MouseEvent) => Unit = (_, _) => ()

  minWidth = 0
  minHeight = 0
  center = viewport
  bottom = horizontal
  right = vertical
  layers.foreach { layer =>
    layer.managed = false
    layer.mouseTransparent = true
  }
  Seq(horizontal, vertical).foreach { bar =>
    bar.visible = false
    bar.managed = false
  }
  viewport.width.onChange { (_, _, _) => requestRedraw() }
  viewport.height.onChange { (_, _, _) => requestRedraw() }
  horizontal.value.onChange { (_, _, value) =>
    if !syncingScrollbars then
      moveTo(geometry.scroll(value.doubleValue, geometry.offsetY))
  }
  vertical.value.onChange { (_, _, value) =>
    if !syncingScrollbars then
      moveTo(geometry.scroll(geometry.offsetX, value.doubleValue))
  }
  viewport.delegate.setOnMouseClicked { event =>
    geometry.positionAt(event.getX, event.getY).foreach { pos =>
      event.getButton match
        case javafx.scene.input.MouseButton.PRIMARY if event.isShiftDown =>
          onShiftClick(pos)
        case javafx.scene.input.MouseButton.PRIMARY   => onPrimaryClick(pos)
        case javafx.scene.input.MouseButton.SECONDARY =>
          onSecondaryClick(pos, new MouseEvent(event))
        case _ => ()
    }
  }
  viewport.delegate.setOnScroll { event =>
    if event.isControlDown then
      if event.getDeltaY > 0 then zoomIn()
      else if event.getDeltaY < 0 then zoomOut()
    else
      val dx = if event.isShiftDown then event.getDeltaY else event.getDeltaX
      val dy = if event.isShiftDown then 0 else event.getDeltaY
      moveTo(geometry.scroll(geometry.offsetX - dx, geometry.offsetY - dy))
    event.consume()
  }

  def updateWarehouse(updated: Warehouse): Unit =
    if !(warehouse eq updated) then
      if warehouse.width != updated.width || warehouse.height != updated.height
      then
        moveTo(
          WarehouseViewport(updated.width, updated.height)
            .resize(viewport.width.value, viewport.height.value)
        )
      warehouse = updated
      tilesDirty = true
      requestRedraw()

  def updateRobots(updated: Seq[RobotSnapshot]): Unit =
    if robots != updated then
      robots = updated
      robotsDirty = true
      requestRedraw()

  def updateSelection(updated: Option[Area]): Unit =
    if selection != updated then
      selection = updated
      selectionDirty = true
      requestRedraw()

  def zoomIn(): Unit = moveTo(geometry.zoom(1.25))
  def zoomOut(): Unit = moveTo(geometry.zoom(1 / 1.25))

  /** Fit remains active on subsequent resizes, until manual zoom is requested.
    */
  def zoomToFit(): Unit = moveTo(geometry.fit)

  private def moveTo(updated: WarehouseViewport): Unit =
    if geometry != updated then
      geometry = updated
      tilesDirty = true
      robotsDirty = true
      selectionDirty = true
      requestRedraw()

  /** Coalesces resize/scroll notifications; there is no continuous render loop.
    */
  private def requestRedraw(): Unit =
    if !redrawPending then
      redrawPending = true
      Platform.runLater(() => if redrawPending then redraw())

  /** Flushes a model update before its view's render Task completes. */
  private[gui] def redraw(): Unit =
    val width = viewport.width.value
    val height = viewport.height.value
    if geometry.width != width || geometry.height != height then
      moveTo(geometry.resize(width, height))
      layers.foreach { layer =>
        layer.width = width
        layer.height = height
      }
    redrawPending = false
    syncScrollbars()
    if geometry.ready then
      if tilesDirty then
        WarehousePainter.warehouse(
          tilesLayer.delegate.getGraphicsContext2D,
          geometry,
          warehouse
        )
      if robotsDirty then
        WarehousePainter.robots(
          robotsLayer.delegate.getGraphicsContext2D,
          geometry,
          robots
        )
      if selectionDirty then
        WarehousePainter.selection(
          selectionLayer.delegate.getGraphicsContext2D,
          geometry,
          selection
        )
      tilesDirty = false
      robotsDirty = false
      selectionDirty = false

  private def syncScrollbars(): Unit =
    syncingScrollbars = true
    def sync(
        bar: ScrollBar,
        offset: Double,
        maximum: Double,
        extent: Double
    ): Unit =
      val needed = !geometry.fitting && maximum > 0
      bar.visible = needed
      bar.managed = needed
      bar.max = maximum
      bar.visibleAmount =
        if maximum > 0 then extent * maximum / (extent + maximum) else 0
      bar.unitIncrement = math.max(16, geometry.cellSize)
      bar.blockIncrement = extent * 0.9
      bar.value = offset
    sync(horizontal, geometry.offsetX, geometry.maxOffsetX, geometry.width)
    sync(vertical, geometry.offsetY, geometry.maxOffsetY, geometry.height)
    syncingScrollbars = false
