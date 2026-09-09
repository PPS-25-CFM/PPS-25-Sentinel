package it.unibo.sentinel.boundary.launcher

import it.unibo.sentinel.boundary.gui.toolkit.Toolkit
import it.unibo.sentinel.control.Engine
import it.unibo.sentinel.control.serialization.FileRepository
import it.unibo.sentinel.core.scenario.Scenario
import it.unibo.sentinel.core.simulation.{Simulation, SimulationId}
import it.unibo.sentinel.core.simulation.Statistics.Report

import scala.concurrent.duration.*

/** Prepares application use cases and coordinates navigation. All navigation
  * and session changes run on the toolkit's UI thread.
  */
final class Application(toolkit: Toolkit)(using repo: FileRepository[Scenario]):
  private val window = toolkit.window
  private var session: Option[SimulationSession] = None

  private lazy val menu =
    val view = toolkit.menu()
    view.onRunSimulation(() => runSimulation())
    view

  /** Opens the application on its main menu.
    */
  def start(): Unit = toolkit.execute:
    window.onClose(() => stopSession())
    showMenu()
    window.open()

  private def runSimulation(): Unit =
    for path <- window.chooseJsonFile() do
      repo.load(path.last) match
        case Right(scenario) => prepareSimulation(scenario)
        case Left(error)     => window.showError(error.toString)

  private def prepareSimulation(scenario: Scenario): Unit =
    stopSession()
    // TODO: to identify the simulation should be used the ScenarioId
    val simulation = Simulation.of(SimulationId("sim-1"), scenario)
    val engine = Engine(simulation, 1.second)
    val view = toolkit.simulation(engine)
    val current = new SimulationSession(
      engine,
      action => toolkit.execute(action())
    )
    session = Some(current)
    view.onMenu(() => showMenu())
    window.show(view)
    current.start(
      onStep = view.render,
      onCompleted = showStatistics
    )

  private def showMenu(): Unit =
    stopSession()
    window.show(menu)

  private def showStatistics(report: Report): Unit =
    stopSession()
    val view = toolkit.statistics()
    view.onMenu(() => showMenu())
    view.render(report)
    window.show(view)

  private def stopSession(): Unit =
    val previous = session
    session = None
    previous.foreach(_.stop())
