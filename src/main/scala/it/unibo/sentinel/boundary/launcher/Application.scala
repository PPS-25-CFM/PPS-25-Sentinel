package it.unibo.sentinel.boundary.launcher

import it.unibo.sentinel.boundary.gui.toolkit.{MenuCommand, Toolkit}
import it.unibo.sentinel.control.Engine
import it.unibo.sentinel.control.serialization.Codec.Validation
import it.unibo.sentinel.control.serialization.FileRepository
import it.unibo.sentinel.control.serialization.JsonSerialization.given
import it.unibo.sentinel.core.scenario.{Scenario, value}
import it.unibo.sentinel.core.simulation.Statistics.Report
import it.unibo.sentinel.core.simulation.{Simulation, SimulationId}
import it.unibo.sentinel.core.warehouse.Warehouse
import monix.eval.Task
import monix.execution.Scheduler
import monix.execution.schedulers.SchedulerService

import scala.concurrent.duration.*

/** This component coordinates the application lifecycle.
  * @param toolkit
  *   the GUI [[Toolkit]] to use.
  */
final class Application(toolkit: Toolkit):

  private val period: FiniteDuration = 1.second
  private val window = toolkit.window
  private val menu = toolkit.menu

  /** Starts the application, showing the menu and waiting for user input.
    */
  def run(): Task[Unit] =
    val session =
      for
        _ <- window.show(menu)
        _ <- Task.race(navigate, window.open())
      yield ()
    session.guarantee(toolkit.shutdown())

  private def navigate: Task[Unit] =
    menu.commands
      .mapEval(handle(_) >> window.show(menu))
      .completedL

  private def handle(command: MenuCommand): Task[Unit] = command match
    case MenuCommand.RunSimulation(scenario) =>
      load(scenario) match
        case Left(failure) => menu.report(failure)
        case Right(loaded) => simulate(loaded)

  private def simulate(scenario: Scenario): Task[Unit] =
    val scheduler = Scheduler.singleThread("engine")
    val id = SimulationId(scenario.id.value)
    val sim = Simulation.of(id, scenario)
    Task(scheduler).bracket(engineOn(sim)): s =>
      Task(s.shutdown())

  private def engineOn(simulation: Simulation)(
      scheduler: SchedulerService
  ): Task[Unit] =
    given Scheduler = scheduler
    val view = toolkit.simulation
    val engine = Engine(simulation, period, view.commands)
    for
      _ <- window.show(view)
      outcome <- Task.race(view.dismissed, engine.run(view.render))
      _ <- outcome.fold(_ => Task.unit, showStatistics)
    yield ()

  private def showStatistics(report: Report): Task[Unit] =
    val view = toolkit.statistics
    for
      _ <- view.render(report)
      _ <- window.show(view)
      _ <- view.dismissed
    yield ()

  private def load(scenario: os.Path): Either[Validation, Scenario] =
    given warehouses: FileRepository[Warehouse] =
      new FileRepository[Warehouse]()
    new FileRepository[Scenario](scenario / os.up).load(scenario.last)
