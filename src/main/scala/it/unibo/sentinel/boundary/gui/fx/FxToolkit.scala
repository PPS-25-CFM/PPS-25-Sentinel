package it.unibo.sentinel.boundary.gui.fx

import it.unibo.sentinel.boundary.gui.toolkit.Toolkit
import it.unibo.sentinel.boundary.gui.fx.FxUtils.onFx
import it.unibo.sentinel.boundary.gui.fx.FxUtils.{defaultWidth, defaultHeight}
import scalafx.scene.Scene
import scalafx.scene.layout.BorderPane
import scalafx.application.Platform
import it.unibo.sentinel.core.mission.{Mission, MissionStatus}
import it.unibo.sentinel.boundary.gui.fx.panels.{
  SideData,
  SidePanel,
  WarehousePanel
}
import it.unibo.sentinel.core.simulation.{StepResult, Event}

/** Toolkit implementation using the fx library
  */
object FxToolkit extends Toolkit:

  Platform.startup(() => ())

  override type W = FxWindow
  override type V[Model] = FxView[Model]

  override val window: W = new FxWindow(Some(defaultWidth), Some(defaultHeight))

  override def simulation: V[StepResult] = new FxView[StepResult]:
    private val root = new BorderPane
    private var warehousePanel: Option[WarehousePanel] = None
    private val leftSidePanel = new SidePanel(Iterable.empty)
    private val rightSidePanel = new SidePanel(Iterable.empty)

    root.left = leftSidePanel
    root.right = rightSidePanel

    override def scene: Scene = new Scene(root)

    override def render(model: StepResult): Unit = onFx:
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
          s"${r.id} at ${r.position} - ${r.status}"
        )
      )
      val eventsData = SideData("Events", model.events.map(parseEvent(_)))
      rightSidePanel.updateData(Iterable(robotsData, eventsData))

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
      val destinationLabel = mission.currentDestination match
        case Some(p) => s" - move to $p"
        case _       => ""
      s"${mission.id}$destinationLabel - ${mission.duration} ticks remaining"

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
