package it.unibo.sentinel.boundary.launcher

import it.unibo.sentinel.boundary.gui.toolkit.Toolkit
import it.unibo.sentinel.control.Engine
import it.unibo.sentinel.control.serialization.FileRepository
import it.unibo.sentinel.control.serialization.JsonSerialization.given
import it.unibo.sentinel.core.scenario.Scenario
import it.unibo.sentinel.core.simulation.{Simulation, SimulationId}
import it.unibo.sentinel.core.simulation.Statistics.Report
import it.unibo.sentinel.core.warehouse.Warehouse

import scala.concurrent.duration.*

/** Prepares application use cases and coordinates navigation. All navigation
  * and session changes run on the toolkit's UI thread.
  */
final class Application(toolkit: Toolkit):
  private val window = toolkit.window

  private given FileRepository[Warehouse] = new FileRepository[Warehouse]
  private val scenarios = new FileRepository[Scenario]

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

  /** Acquires the scenario before preparing the simulation use case.
    */
  private def runSimulation(): Unit =
    for path <- window.chooseJsonFile() do
      scenarios.load(path.last) match
        case Right(scenario) => prepareSimulation(scenario)
        case Left(error)     => window.showError(error.toString)

  /** Creates and connects the components needed to run a simulation.
    */
  private def prepareSimulation(scenario: Scenario): Unit =
    stopSession()

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
