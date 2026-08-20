package it.unibo.sentinel.boundary.gui.fx

import it.unibo.sentinel.boundary.gui.toolkit.Toolkit
import it.unibo.sentinel.boundary.gui.fx.FxUtils.onFx
import it.unibo.sentinel.boundary.gui.fx.panels.WarehousePanel
import it.unibo.sentinel.boundary.gui.fx.FxUtils.defaultWidth
import it.unibo.sentinel.boundary.gui.fx.FxUtils.defaultHeight
import scalafx.scene.Scene
import scalafx.scene.layout.BorderPane
import scalafx.application.Platform
import it.unibo.sentinel.core.scenario.Scenario
import it.unibo.sentinel.core.mission.MissionStatus
import it.unibo.sentinel.core.mission.Mission
import it.unibo.sentinel.boundary.gui.fx.panels.SideData
import it.unibo.sentinel.boundary.gui.fx.panels.SidePanel

/** Toolkit implementation using the fx library
  */
object FxToolkit extends Toolkit:

  Platform.startup(() => ())

  override type W = FxWindow
  override type V[Model] = FxView[Model]

  override val window: W = new FxWindow(Some(defaultWidth), Some(defaultHeight))

  override def simulation: V[Scenario] = new FxView[Scenario]:

    private val root = new BorderPane

    override def scene: Scene = new Scene(root)

    override def render(model: Scenario): Unit = onFx:
      root.center = new WarehousePanel(model.warehouse, model.spawns)
      val missions =
        for status <- MissionStatus.values
        yield SideData(
          status.toString(),
          filterAndParse(status, model.missions)
        )
      root.left = new SidePanel(missions)
      val robots = SideData("Robots", model.spawns.map(r => s"${r.id} at ${r.at} - TODO: STATUS"))
      val events = SideData("Events", Iterable.empty)
      root.right = new SidePanel(Iterable(robots, events))
      window.resize()

    /** @param status
      *   used to filter the missions
      * @return
      *   a list of descriptions, one for each of the filtered missions
      */
    private def filterAndParse(
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
