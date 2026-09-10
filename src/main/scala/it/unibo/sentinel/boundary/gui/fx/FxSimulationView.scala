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
import it.unibo.sentinel.core.warehouse.Tile
import monix.eval.Task
import monix.execution.{CancelablePromise, Scheduler}
import monix.reactive.Observable
import monix.reactive.subjects.ConcurrentSubject
import scalafx.Includes.{eventClosureWrapperWithParam, jfxKeyEvent2sfx}
import scalafx.scene.Scene
import scalafx.scene.input.{KeyCode, KeyEvent}
import scalafx.scene.layout.BorderPane

/** */
final class FxSimulationView extends FxView with SimulationView:
  import Scheduler.Implicits.global

  private val exit = CancelablePromise[Unit]()
  private val subject = ConcurrentSubject.publish[Command]
  private val root = new BorderPane
  private var warehousePanel: Option[WarehousePanel] = None
  private val leftSidePanel = new SidePanel(Iterable.empty)
  private val rightSidePanel = new SidePanel(Iterable.empty)

  root.left = leftSidePanel
  root.right = rightSidePanel
  root.top = FxControls.backToMenu(exit)

  override def commands: Observable[Command] = subject

  override def dismissed: Task[Unit] = Task.fromCancelablePromise(exit)

  /** @return the JavaFX scene with key bindings. */
  override lazy val scene: Scene =
    val s = new Scene(root)
    s.onKeyPressed = (e: KeyEvent) =>
      e.code match
        case KeyCode.P => emit(Command.Pause)
        case KeyCode.R => emit(Command.Resume)
        case KeyCode.A => emit(Command.Back)
        case KeyCode.D => emit(Command.Next)
        case _         => ()
    s

  /** @param command
    *   the command requested by the user.
    */
  private def emit(command: Command): Unit =
    val _ = subject.onNext(command)

  /** Describes the rendering of the current simulation [[StepResult]] onto the
    * warehouse and side panels.
    */
  override def render(model: StepResult): Task[Unit] = onFx:
    val panel = warehousePanel.getOrElse {
      val p = new WarehousePanel(model.snapshot.warehouse)
      root.center = p
      warehousePanel = Some(p)
      p
    }
    panel.updateRobots(model.snapshot.robots)

    val sideMissions =
      for status <- MissionStatus.values
      yield SideData(
        status.toString(),
        filterAndParseMissions(status, model.snapshot.missions)
      )
    leftSidePanel.updateData(sideMissions)

    val robotsData = SideData(
      "Robots",
      model.snapshot.robots.map(r =>
        val assignment = model.snapshot.missions
          .find(_.carrier.contains(r.id))
          .map(m => s" - on ${m.id}")
          .getOrElse(" - free")
        s"${r.id} at ${r.position} - ${r.status}$assignment"
      )
    )
    val warehouseData = SideData(
      "Warehouse",
      describeWarehouse(model.snapshot)
    )
    val eventsData = SideData("Events", model.events.map(parseEvent(_)))
    rightSidePanel.updateData(
      Iterable(robotsData, warehouseData, eventsData)
    )

  /** @param status
    *   used to filter the missions
    * @return
    *   a list of descriptions, one for each of the filtered missions
    */
  private def filterAndParseMissions(
      status: MissionStatus,
      missions: Seq[Mission]
  ): Iterable[String] =
    missions.filter(_.status == status).map(parseMission)

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
