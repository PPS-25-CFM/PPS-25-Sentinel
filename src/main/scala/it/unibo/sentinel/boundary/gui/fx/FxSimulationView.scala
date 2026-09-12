package it.unibo.sentinel.boundary.gui.fx

import it.unibo.sentinel.boundary.gui.fx.FxUtils.onFx
import it.unibo.sentinel.boundary.gui.fx.panels.{
  SideData,
  SidePanel,
  WarehousePanel
}
import it.unibo.sentinel.boundary.gui.toolkit.SimulationView
import it.unibo.sentinel.control.Engine.Command
import it.unibo.sentinel.core.mission.{Action, Mission, MissionStatus}
import it.unibo.sentinel.core.simulation.{Event, Snapshot, StepResult}
import it.unibo.sentinel.core.warehouse.{Tile, Warehouse}
import monix.eval.Task
import scalafx.Includes.{eventClosureWrapperWithParam, jfxKeyEvent2sfx}
import scalafx.scene.Scene
import scalafx.scene.control.{Button, SplitPane}
import scalafx.scene.input.{KeyCode, KeyEvent}
import scalafx.scene.layout.BorderPane

/** */
final class FxSimulationView extends FxView with SimulationView:

  private val root = new BorderPane
  private var warehousePanel: Option[WarehousePanel] = None
  private val leftSidePanel = new SidePanel(Iterable.empty)
  private val rightSidePanel = new SidePanel(Iterable.empty)

  private val split = new SplitPane:
    minWidth = 0
    minHeight = 0
  private var showMissions = true
  private var showDetails = true
  private var leftFraction = 0.2
  private var rightFraction = 0.2
  private var describedWarehouse = Option.empty[Warehouse]
  private var warehouseDescription = Seq.empty[String]

  root.styleClass += "warehouse-view"
  root.center = split
  root.top = FxControls.toolbar(
    Seq(FxControls.backToMenu(() => dismiss())) ++ zoomControls ++ Seq(
      FxControls.button("Pause (P)", () => emit(Command.Pause)),
      FxControls.button("Resume (R)", () => emit(Command.Resume)),
      FxControls.button("Previous (A)", () => emit(Command.Back)),
      FxControls.button("Next (D)", () => emit(Command.Next)),
      FxControls.button("Missions", () => toggleSidebar(missions = true)),
      FxControls.button("Details", () => toggleSidebar(missions = false))
    )
  )

  private def toggleSidebar(missions: Boolean): Unit =
    val positions = split.delegate.getDividerPositions.toSeq
    if showMissions then
      positions.headOption.foreach(value => leftFraction = value)
    if showDetails then
      positions.lastOption.foreach(value => rightFraction = 1 - value)
    if missions then showMissions = !showMissions
    else showDetails = !showDetails
    arrangePanels()

  private def arrangePanels(): Unit =
    warehousePanel.foreach { panel =>
      val panels: Seq[scalafx.scene.Node] =
        Option.when(showMissions)(leftSidePanel).toSeq ++ Seq(panel) ++
          Option.when(showDetails)(rightSidePanel).toSeq
      val _ = split.items.setAll(panels.map(_.delegate)*)
      val left = math.max(0.1, math.min(0.35, leftFraction))
      val right = math.max(0.1, math.min(0.35, rightFraction))
      val positions = Option.when(showMissions)(left).toSeq ++ Option
        .when(showDetails)(1 - right)
        .toSeq
      split.delegate.setDividerPositions(positions*)
    }

  /** @return the JavaFX scene with key bindings. */
  override lazy val scene: Scene =
    val s = FxControls.style(new Scene(root))
    s.onKeyPressed = (e: KeyEvent) =>
      e.code match
        case KeyCode.P => emit(Command.Pause)
        case KeyCode.R => emit(Command.Resume)
        case KeyCode.A => emit(Command.Back)
        case KeyCode.D => emit(Command.Next)
        case _         => ()
    s

  /** @return the zoom controls, wired to the current panel once it exists. */
  private def zoomControls: Seq[Button] =
    FxControls.zoomControls(
      zoomIn = () => warehousePanel.foreach(_.zoomIn()),
      zoomOut = () => warehousePanel.foreach(_.zoomOut()),
      zoomToFit = () => warehousePanel.foreach(_.zoomToFit())
    )

  /** Describes the rendering of the current simulation [[StepResult]] onto the
    * warehouse and side panels.
    */
  override def render(model: StepResult): Task[Unit] =
    Task(prepareSidebarData(model)).flatMap { (missions, details) =>
      onFx {
        val panel = warehousePanel.getOrElse {
          val created = new WarehousePanel(model.snapshot.warehouse)
          warehousePanel = Some(created)
          arrangePanels()
          created
        }
        panel.updateWarehouse(model.snapshot.warehouse)
        panel.updateRobots(model.snapshot.robots)
        leftSidePanel.updateData(missions)
        rightSidePanel.updateData(details)
        panel.redraw()
      }
    }

  /** Runs on the render caller's scheduler, before handing UI changes to
    * JavaFX. Application serialises render tasks, so this cache needs no extra
    * thread.
    */
  private def prepareSidebarData(
      model: StepResult
  ): (Seq[SideData], Seq[SideData]) =
    val snapshot = model.snapshot
    val byStatus = snapshot.missions.groupBy(_.status)
    val missions = MissionStatus.values.toSeq.map(status =>
      SideData(
        status.toString,
        byStatus.getOrElse(status, Seq.empty).map(parseMission)
      )
    )
    val assignments = snapshot.missions
      .flatMap(m => m.carrier.map(_ -> m.id))
      .groupMap(_._1)(_._2)
      .flatMap { (robot, ids) => ids.headOption.map(robot -> _) }
    val robots = SideData(
      "Robots",
      snapshot.robots.map { robot =>
        val assignment =
          assignments.get(robot.id).fold(" - free")(id => s" - on $id")
        s"${robot.id} at ${robot.position} - ${robot.status}$assignment"
      }
    )
    if !describedWarehouse.exists(_ eq snapshot.warehouse) then
      describedWarehouse = Some(snapshot.warehouse)
      warehouseDescription = describeWarehouse(snapshot).toSeq
    val details = Seq(
      robots,
      SideData("Warehouse", warehouseDescription),
      SideData("Events", model.events.map(parseEvent))
    )
    (missions, details)

  /** @param mission
    *   the mission to extract the description from
    * @return
    *   a brief description of the given mission
    */
  private def parseMission(mission: Mission): String =
    val actions = mission.task.actions.toSeq
    val taskLabel =
      if actions.isEmpty then "done"
      else actions.map(parseAction).mkString(" -> ")
    val currentLabel = mission.currentTarget match
      case Some(p) => s" - next $p"
      case _       => ""
    s"${mission.id}: $taskLabel$currentLabel - ${mission.deadline} ticks remaining"

  /** @param action
    *   the action to describe.
    * @return
    *   a short textual description.
    */
  private def parseAction(action: Action): String = action match
    case Action.Move(to)         => s"move to $to"
    case Action.PickUp(item, at) => s"pick $item at $at"
    case Action.Drop(item, at)   => s"drop $item at $at"

  /** @param snapshot
    *   the current simulation snapshot.
    * @return
    *   a sorted list of shelf and loading bay descriptions, or a placeholder if
    *   none exist.
    */
  private def describeWarehouse(snapshot: Snapshot): Iterable[String] =
    val shelves = snapshot.warehouse.tiles.collect:
      case (pos, Tile.Shelf(item)) => s"Shelf $item at $pos"
    val bays = snapshot.warehouse.tiles.collect:
      case (pos, _: Tile.LoadingBay) => s"Bay at $pos"
    (shelves.sorted ++ bays.sorted) match
      case Seq() => Seq("no shelves/bays")
      case other => other

  /** @param event
    *   the event to extract the description from
    * @return
    *   a brief description of the given event
    */
  private def parseEvent(event: Event): String =
    event match
      case Event.MissionAssigned(robotId, missionId) =>
        s"Assigned $missionId to $robotId"
      case Event.MissionCompleted(missionId) => s"$missionId completed"
      case Event.MissionFailed(missionId)    => s"$missionId failed"
      case Event.RobotRouted(robotId, path)  =>
        val pString = path.map(_.toString()).mkString("->")
        s"$robotId following path $pString"
      case Event.RobotMoved(robotId, from, to) =>
        s"$robotId moved from $from to $to"
      case Event.RobotBlocked(robotId, at) => s"$robotId blocked at $at"
      case Event.RobotUnblocked(robotId)   => s"$robotId unblocked"
      case Event.ItemPicked(robotId, missionId, item, at) =>
        s"$robotId picked $item for $missionId at $at"
      case Event.ItemDropped(robotId, missionId, item, at) =>
        s"$robotId dropped $item for $missionId at $at"
