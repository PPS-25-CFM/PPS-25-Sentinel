package it.unibo.sentinel.boundary.gui.fx

import it.unibo.sentinel.boundary.gui.fx.FxUtils.onFx
import it.unibo.sentinel.boundary.gui.fx.panels.WarehousePanel
import it.unibo.sentinel.boundary.gui.toolkit.WarehouseEditorView
import it.unibo.sentinel.control.WarehouseEditor
import it.unibo.sentinel.control.WarehouseEditor.Command
import it.unibo.sentinel.core.item.Item
import it.unibo.sentinel.core.simulation.Tick
import it.unibo.sentinel.core.warehouse.{Area, Position, Tile, Warehouse}
import monix.eval.Task
import scalafx.scene.Scene
import scalafx.scene.control.{
  Alert,
  ContextMenu,
  MenuItem,
  Menu as FxSubMenu,
  Label,
  TextInputDialog
}
import scalafx.scene.control.Alert.AlertType
import scalafx.scene.input.MouseEvent
import scalafx.scene.layout.BorderPane

/** [[WarehouseEditorView]] implementation based on the fx library.
  */
final class FxWarehouseEditorView extends FxView with WarehouseEditorView:

  private val root = new BorderPane
  private var panel: Option[WarehousePanel] = None
  private var selection: Option[Area] = None

  root.styleClass += "warehouse-view"
  root.top = FxControls.toolbar(
    Seq(FxControls.backToMenu(() => dismiss())) ++ zoomControls
  )
  root.bottom = new Label(
    "Click: select  ·  Shift+click: extend  ·  Right-click: edit  ·  Ctrl+scroll: zoom"
  ):
    styleClass += "warehouse-hint"
    wrapText = true
    minWidth = 0

  override lazy val scene: Scene = FxControls.style(new Scene(root))

  override def render(state: WarehouseEditor.State): Task[Unit] = onFx:
    selection = state.selection
    val p = panelFor(state.warehouse)
    p.updateWarehouse(state.warehouse)
    p.updateSelection(state.selection)
    p.redraw()

  private def panelFor(warehouse: Warehouse): WarehousePanel =
    panel.getOrElse {
      val p = new WarehousePanel(warehouse)
      p.onPrimaryClick = (pos: Position) => emit(Command.Select(pos))
      p.onShiftClick = (pos: Position) => emit(Command.ExtendSelection(pos))
      p.onSecondaryClick =
        (pos: Position, event: MouseEvent) => showTileMenu(pos, event)
      root.center = p
      panel = Some(p)
      p
    }

  private def zoomControls: Seq[scalafx.scene.control.Button] =
    FxControls.zoomControls(
      zoomIn = () => panel.foreach(_.zoomIn()),
      zoomOut = () => panel.foreach(_.zoomOut()),
      zoomToFit = () => panel.foreach(_.zoomToFit())
    )

  private def promptCost(tileName: String): Option[Int] =
    val dialog = new TextInputDialog(defaultValue = "1"):
      title = tileName
      headerText = "Traversal cost"
      contentText = "Cost:"
    dialog.showAndWait().flatMap(_.toIntOption)

  private def applyCostTile(tileName: String)(build: Tick => Tile): Unit =
    promptCost(tileName).foreach: raw =>
      Tile.validateCost(raw) match
        case Right(cost) => emit(Command.Apply(build(cost)))
        case Left(error) => reportInvalidTile(error)

  private def reportInvalidTile(error: Tile.Validation): Unit =
    onFx {
      val alert = new Alert(AlertType.Error):
        title = "Invalid tile"
        headerText = "The tile could not be applied"
        contentText = s"Invalid tile: $error"
      val _ = alert.showAndWait()
    }.runAsyncAndForget

  private def showTileMenu(pos: Position, event: MouseEvent): Unit =
    if !selection.exists(area =>
        pos.x >= math.min(area.corner.x, area.opposite.x) &&
          pos.x <= math.max(area.corner.x, area.opposite.x) &&
          pos.y >= math.min(area.corner.y, area.opposite.y) &&
          pos.y <= math.max(area.corner.y, area.opposite.y)
      )
    then emit(Command.Select(pos))

    val floor = new MenuItem("Floor"):
      onAction = _ => applyCostTile("Floor")(Tile.Floor(_))
    val loadingBay = new MenuItem("Loading Bay"):
      onAction = _ => applyCostTile("Loading Bay")(Tile.LoadingBay(_))
    val remove = new MenuItem("Remove"):
      onAction = _ => emit(Command.Remove)
    val shelf = new FxSubMenu("Shelf"):
      items = Item.values.toSeq.map { item =>
        new MenuItem(item.toString):
          onAction = _ => emit(Command.Apply(Tile.Shelf(item)))
      }

    val menu = new ContextMenu(floor, shelf, loadingBay, remove)
    panel.foreach(p => menu.show(p, event.screenX, event.screenY))
