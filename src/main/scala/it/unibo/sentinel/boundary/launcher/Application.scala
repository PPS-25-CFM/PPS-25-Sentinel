package it.unibo.sentinel.boundary.launcher

import it.unibo.sentinel.boundary.gui.toolkit.{MenuCommand, Toolkit}
import it.unibo.sentinel.control.{Engine, WarehouseEditor}
import it.unibo.sentinel.control.serialization.Codec.Validation
import it.unibo.sentinel.control.serialization.FileRepository
import it.unibo.sentinel.control.serialization.JsonSerialization.given
import it.unibo.sentinel.core.scenario.{Scenario, value}
import it.unibo.sentinel.core.simulation.Statistics.Report
import it.unibo.sentinel.core.simulation.{Simulation, SimulationId}
import it.unibo.sentinel.core.warehouse.{Warehouse, WarehouseId}
import monix.eval.Task
import monix.execution.Scheduler
import monix.execution.schedulers.SchedulerService

import scala.concurrent.duration.*
import scala.util.{Try, Failure, Success}

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
      loadScenario(scenario).fold(menu.report, simulate)
    case MenuCommand.NewWarehouse(id, width, height) =>
      Try(Warehouse.empty(WarehouseId(id), width, height)) match
        case Failure(_) =>
          val failure = Validation.WarehouseValidation(
            Warehouse.Validation.InvalidSize(width, height)
          )
          menu.report(failure)
        case Success(warehouse) =>
          editWarehouse(warehouse, FileRepository.folderPath)
    case MenuCommand.OpenWarehouse(warehouse) =>
      loadWarehouse(warehouse).fold(
        menu.report,
        editWarehouse(_, warehouse / os.up)
      )

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

  private def editWarehouse(initial: Warehouse, root: os.Path): Task[Unit] =
    val view = toolkit.editor
    for
      _ <- window.show(view)
      _ <- view.render(WarehouseEditor.State(initial))
      edited <-
        view.commands
          .scan(WarehouseEditor.State(initial))(WarehouseEditor.reduce)
          .mapEval(state => view.render(state).map(_ => state))
          .takeUntilEval(view.dismissed)
          .lastOrElseL(WarehouseEditor.State(initial))
      outcome = new FileRepository[Warehouse](root).save(edited.warehouse)
      _ <- outcome.fold(menu.report, _ => Task.unit)
    yield ()

  private def loadScenario(scenario: os.Path): Either[Validation, Scenario] =
    given warehouses: FileRepository[Warehouse] =
      new FileRepository[Warehouse]()
    new FileRepository[Scenario](scenario / os.up).load(scenario.last)

  private def loadWarehouse(warehouse: os.Path): Either[Validation, Warehouse] =
    new FileRepository[Warehouse](warehouse / os.up).load(warehouse.last)
