package it.unibo.sentinel.boundary.launcher

import it.unibo.sentinel.boundary.gui.toolkit.{EditorView, MenuCommand, Toolkit}
import it.unibo.sentinel.control.{
  Editor,
  Engine,
  ScenarioEditor,
  WarehouseEditor
}
import it.unibo.sentinel.control.serialization.Codec.Validation
import it.unibo.sentinel.control.serialization.{FileRepository, Repository}
import it.unibo.sentinel.control.serialization.JsonSerialization.given
import it.unibo.sentinel.core.scenario.{Scenario, ScenarioId, value}
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
    case MenuCommand.NewScenario(id, warehouse) =>
      val root = warehouse / os.up
      if os.exists(root / s"$id.json") then
        menu.report(Validation.FileAlreadyExists(s"$id.json"))
      else
        loadWarehouse(warehouse).fold(
          menu.report,
          w => editScenario(Scenario.in(w).withId(ScenarioId(id)), root)
        )
    case MenuCommand.OpenScenario(scenario) =>
      loadScenario(scenario).fold(
        menu.report,
        editScenario(_, scenario / os.up)
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

  private def edit(editor: Editor)(
      view: toolkit.V & EditorView[editor.State, editor.Command],
      initial: editor.State,
      repository: Repository[String, editor.Model]
  ): Task[Unit] =
    for
      _ <- window.show(view)
      _ <- view.render(initial)
      edited <-
        editor
          .execute(initial, view.commands)
          .mapEval(state => view.render(state).map(_ => state))
          .takeUntilEval(view.dismissed)
          .lastOrElseL(initial)
      outcome = repository.save(editor.model(edited))
      _ <- outcome.fold(menu.report, _ => Task.unit)
    yield ()

  private def editWarehouse(initial: Warehouse, root: os.Path): Task[Unit] =
    edit(WarehouseEditor)(
      toolkit.editor,
      WarehouseEditor.State(initial),
      new FileRepository[Warehouse](root)
    )

  private def editScenario(initial: Scenario, root: os.Path): Task[Unit] =
    given FileRepository[Warehouse] = new FileRepository[Warehouse](root)
    edit(ScenarioEditor)(
      toolkit.scenarioEditor,
      ScenarioEditor.State(initial),
      new FileRepository[Scenario](root)
    )

  private def loadScenario(scenario: os.Path): Either[Validation, Scenario] =
    def from(warehouseRoot: os.Path): Either[Validation, Scenario] =
      given FileRepository[Warehouse] =
        new FileRepository[Warehouse](warehouseRoot)
      new FileRepository[Scenario](scenario / os.up).load(scenario.last)
    from(scenario / os.up) match
      case Left(_: Validation.FileNotFound) => from(FileRepository.folderPath)
      case outcome                          => outcome

  private def loadWarehouse(warehouse: os.Path): Either[Validation, Warehouse] =
    new FileRepository[Warehouse](warehouse / os.up).load(warehouse.last)
