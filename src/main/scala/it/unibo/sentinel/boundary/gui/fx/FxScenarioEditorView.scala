package it.unibo.sentinel.boundary.gui.fx

import it.unibo.sentinel.boundary.gui.fx.FxUtils.onFx
import it.unibo.sentinel.boundary.gui.fx.panels.WarehousePanel
import it.unibo.sentinel.boundary.gui.toolkit.ScenarioEditorView
import it.unibo.sentinel.control.ScenarioEditor
import it.unibo.sentinel.control.ScenarioEditor.Command
import it.unibo.sentinel.core.mission.{Action, Mission, Priority}
import it.unibo.sentinel.core.robot.RobotStatus
import it.unibo.sentinel.core.scenario.{
  Policies,
  RobotClass,
  Scenario,
  Spawn,
  Validation
}
import it.unibo.sentinel.core.simulation.{RobotSnapshot, Tick}
import it.unibo.sentinel.core.warehouse.{Area, Position, Tile, Warehouse}
import monix.eval.Task
import scalafx.collections.ObservableBuffer
import scalafx.scene.{Node, Scene}
import scalafx.scene.control.{
  ComboBox,
  ContextMenu,
  Label,
  ListView,
  MenuItem,
  ScrollPane,
  SplitPane,
  TextField,
  Tooltip,
  Menu as FxSubMenu
}
import scalafx.scene.input.MouseEvent
import scalafx.scene.layout.{BorderPane, VBox}

import scala.jdk.OptionConverters.*

/** Configures a scenario through cell menus and an atomic pick-then-drop flow.
  */
final class FxScenarioEditorView extends FxView with ScenarioEditorView:

  private val root = new BorderPane
  private var panel = Option.empty[WarehousePanel]
  private var displayed = Option.empty[ScenarioEditor.State]
  private val hint = new Label:
    styleClass += "warehouse-hint"
    wrapText = true
    minWidth = 0
  private val cancel = FxControls.button(
    "Cancel delivery (Esc)",
    () => emit(Command.CancelDelivery)
  )
  private val robots = new Entries[Spawn](
    "Robots",
    s => s"${s.id}: ${s.ofClass} at ${s.at}",
    s => Command.RemoveRobot(s.id)
  )
  private val missions = new Entries[Mission](
    "Missions",
    describeMission,
    m => Command.UnloadMission(m.id)
  )
  private val routing = new PolicyChoice(
    "Routing",
    Policies.Routing.values.toSeq,
    Command.ChooseRouting.apply
  )
  private val assignment = new PolicyChoice(
    "Assignment",
    Policies.Assignment.values.toSeq,
    Command.ChooseAssigmnment.apply
  )
  private val collisionSelection = new PolicyChoice(
    "Collision selection",
    Policies.CollisionSelection.values.toSeq,
    Command.ChooseCollisionSelection.apply
  )
  private val collisionAvoidance = new PolicyChoice(
    "Collision avoidance",
    Policies.CollisionAvoidance.values.toSeq,
    Command.ChooseCollisionAvoidance.apply
  )
  private val seed = new TextField:
    promptText = "Random seed"
  private val applySeed = FxControls.button(
    "Apply seed",
    () => seed.text.value.toLongOption.foreach(s => emit(Command.Reseed(s)))
  )
  seed.text.onChange { (_, _, _) =>
    applySeed.disable = seed.text.value.toLongOption.isEmpty
  }

  private val details = new ScrollPane:
    styleClass += "warehouse-sidebar"
    fitToWidth = true
    minWidth = 240
    prefWidth = 320
    content = new VBox:
      styleClass += "warehouse-sections"
      children = Seq(
        robots.content,
        missions.content,
        routing.content,
        assignment.content,
        collisionSelection.content,
        collisionAvoidance.content,
        heading("Random seed"),
        seed,
        applySeed
      )
  private val split = new SplitPane:
    minWidth = 0
    minHeight = 0

  root.styleClass += "warehouse-view"
  root.center = split
  root.bottom = hint
  root.top = FxControls.toolbar(
    Seq(
      FxControls
        .button("Save and return to menu", () => dismiss()),
      cancel
    ) ++ FxControls.zoomControls(
      () => panel.foreach(_.zoomIn()),
      () => panel.foreach(_.zoomOut()),
      () => panel.foreach(_.zoomToFit())
    )
  )

  override lazy val scene: Scene =
    val s = FxControls.style(new Scene(root))
    s.delegate.setOnKeyPressed { event =>
      if event.getCode == javafx.scene.input.KeyCode.ESCAPE then
        emit(Command.CancelDelivery)
        event.consume()
    }
    s

  override def render(state: ScenarioEditor.State): Task[Unit] = onFx:
    val p = panelFor(state.scenario.warehouse)
    p.updateRobots(
      state.scenario.spawns.map(s =>
        RobotSnapshot(s.id, RobotStatus.Idle, s.at, None)
      )
    )
    p.updateSelection(
      state.selection.highlighted.map(at => Area(at, at))
    )
    p.redraw()
    robots.update(state.scenario.spawns)
    missions.update(state.scenario.missions)
    routing.update(state.scenario.routing)
    assignment.update(state.scenario.assignment)
    collisionSelection.update(state.scenario.collisionSelection)
    collisionAvoidance.update(state.scenario.collisionAvoidance)
    if !displayed.exists(_.scenario.seed == state.scenario.seed) then
      seed.text = state.scenario.seed.toString
    cancel.disable = state.selection.origin.isEmpty
    hint.text = state.fail
      .map(describeFailure)
      .getOrElse(
        state.selection.origin.fold(
          "Click: select  ·  Right-click: edit  ·  Ctrl+scroll: zoom"
        )(from =>
          s"Delivery from $from: click a loading bay, or cancel with Esc."
        )
      )
    displayed = Some(state)

  private def panelFor(warehouse: Warehouse): WarehousePanel =
    panel.getOrElse {
      val p = new WarehousePanel(warehouse)
      p.onPrimaryClick = select
      p.onShiftClick = select
      p.onSecondaryClick = showMenu
      val _ = split.items.setAll(p.delegate, details.delegate)
      split.delegate.setDividerPositions(0.75)
      panel = Some(p)
      p
    }

  private def select(at: Position): Unit =
    emit(Command.Select(at))
    displayed.foreach { state =>
      if state.selection.origin.isDefined && state.scenario.warehouse
          .isLoadingBay(at)
      then
        val summary = state.selection.origin.toSeq
          .map(from =>
            state.scenario.warehouse.tileAt(from) match
              case Some(Tile.Shelf(item)) =>
                s"$item: shelf $from → loading bay $at"
              case _ => s"Shelf $from → loading bay $at"
          )
          .mkString
        missionDetails("New delivery", summary) match
          case Some((duration, priority)) =>
            emit(Command.LoadDelivery(duration, priority))
          case None => emit(Command.CancelDelivery)
    }

  private def showMenu(at: Position, event: MouseEvent): Unit =
    displayed.foreach { state =>
      val entries = if state.selection.origin.isDefined then
        Seq(menuItem("Cancel delivery", () => emit(Command.CancelDelivery)))
      else
        emit(Command.Select(at))
        cellActions(state.scenario, at)
      if entries.nonEmpty then
        val menu = new ContextMenu(entries*)
        panel.foreach(p => menu.show(p, event.screenX, event.screenY))
    }

  private def cellActions(scenario: Scenario, at: Position): Seq[MenuItem] =
    val warehouse = scenario.warehouse
    val robot = scenario.spawns.find(_.at == at)
    val placement = robot.fold {
      Option
        .when(warehouse.isTraversable(at)) {
          new FxSubMenu("Add robot"):
            items = RobotClass.values.toSeq.map(cls =>
              menuItem(cls.toString, () => emit(Command.PlaceRobot(at, cls)))
            )
        }
        .toSeq
    }(s => Seq(menuItem("Remove robot", () => emit(Command.RemoveRobot(s.id)))))
    val relocation = Option
      .when(warehouse.isTraversable(at)) {
        menuItem(
          "New relocation",
          () =>
            missionDetails("New relocation", s"Destination: $at").foreach {
              (duration, priority) =>
                emit(Command.LoadRelocation(at, duration, priority))
            }
        )
      }
      .toSeq
    val delivery = Option
      .when(warehouse.isShelf(at)) {
        val item =
          menuItem("New delivery", () => emit(Command.BeginDelivery(at)))
        item.disable = !warehouse.tiles.exists { case (_, tile) =>
          tile match
            case _: Tile.LoadingBay => true
            case _                  => false
        }
        item
      }
      .toSeq
    placement ++ relocation ++ delivery

  private def menuItem(label: String, action: () => Unit): MenuItem =
    new MenuItem(label):
      onAction = _ => action()

  private def missionDetails(
      title: String,
      summary: String
  ): Option[(Tick, Priority)] =
    val duration = new TextField:
      text = "100"
    val priority = new ComboBox[Int](
      ObservableBuffer.from(Priority.lowest.value to Priority.highest.value)
    ):
      value = Priority.normal.value
    val dialog =
      new javafx.scene.control.Dialog[javafx.scene.control.ButtonType]()
    val ok = javafx.scene.control.ButtonType.OK
    dialog.initOwner(scene.window())
    dialog.setTitle(title)
    dialog.setHeaderText(summary)
    dialog.getDialogPane.getButtonTypes
      .addAll(ok, javafx.scene.control.ButtonType.CANCEL)
    dialog.getDialogPane.setContent(
      new VBox(
        8,
        new Label("Duration in ticks (positive integer)"),
        duration,
        new Label("Priority (1–10, higher first)"),
        priority
      ).delegate
    )
    def values: Option[(Tick, Priority)] = for
      ticks <- duration.text.value.toIntOption.filter(_ > 0)
      level <- Priority.from(priority.value.value)
    yield (Tick(ticks), level)
    duration.text.onChange { (_, _, _) =>
      Option(dialog.getDialogPane.lookupButton(ok))
        .foreach(_.setDisable(values.isEmpty))
    }
    dialog.showAndWait().toScala.filter(_ == ok).flatMap(_ => values)

  private def heading(text: String): Label = new Label(text):
    styleClass += "warehouse-section-title"

  private def describeMission(mission: Mission): String =
    val actions = mission.task.actions
      .map {
        case Action.Move(to)         => s"move to $to"
        case Action.PickUp(item, at) => s"pick $item at $at"
        case Action.Drop(item, at)   => s"drop $item at $at"
      }
      .mkString(" → ")
    s"${mission.id}: $actions · ${mission.deadline.value} ticks · priority ${mission.priority.value}"

  private def describeFailure(failure: Validation): String = failure match
    case Validation.PositionOccupied(at) =>
      s"Cell $at already contains a robot."
    case Validation.NotFloorTile(at) =>
      s"Choose a traversable cell; $at is not traversable."
    case Validation.RobotAlreadyExists(id)   => s"Robot $id already exists."
    case Validation.MissionAlreadyExists(id) => s"Mission $id already exists."
    case Validation.NotShelfTile(at)         =>
      s"Choose a shelf; there is no shelf at $at."
    case Validation.NotLoadingBay(at) =>
      s"Choose a loading bay; $at is not a loading bay."
    case Validation.ItemMismatch(at, expected, found) =>
      s"Shelf $at stores $found, not $expected."

  /** Both lists retain typed values and their selection across unrelated edits.
    */
  private final class Entries[A](
      title: String,
      describe: A => String,
      remove: A => Command
  ):
    private val buffer = ObservableBuffer.empty[A]
    private val list = new ListView[A](buffer):
      styleClass += "warehouse-list"
      prefHeight = 160
      minWidth = 0
    private val button = FxControls.button(
      "Remove selected",
      () =>
        Option(list.delegate.getSelectionModel.getSelectedItem).foreach(value =>
          emit(remove(value))
        )
    )
    button.disable = true
    list.delegate.getSelectionModel.selectedItemProperty.addListener(
      (_, _, value) => button.disable = Option(value).isEmpty
    )
    list.delegate.setCellFactory(_ =>
      new javafx.scene.control.ListCell[A]:
        override protected def updateItem(item: A, empty: Boolean): Unit =
          super.updateItem(item, empty)
          val text = if empty then "" else Option(item).fold("")(describe)
          setText(text)
          setTooltip(new Tooltip(text).delegate)
    )
    val content: Node = new VBox(6, heading(title), list, button)

    def update(values: Seq[A]): Unit =
      if buffer.toSeq != values then
        val selected = Option(list.delegate.getSelectionModel.getSelectedItem)
        buffer.clear()
        buffer ++= values
        selected
          .filter(values.contains)
          .foreach(list.delegate.getSelectionModel.select)

  private final class PolicyChoice[A](
      title: String,
      values: Seq[A],
      choose: A => Command
  ):
    private var rendering = false
    private val combo = new ComboBox[A](ObservableBuffer.from(values))
    combo.maxWidth = Double.MaxValue
    combo.value.onChange { (_, _, value) =>
      if !rendering then Option(value).foreach(v => emit(choose(v)))
    }
    val content: Node = new VBox(6, heading(title), combo)

    def update(value: A): Unit =
      rendering = true
      combo.value = value
      rendering = false
