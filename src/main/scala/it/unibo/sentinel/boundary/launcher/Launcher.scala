package it.unibo.sentinel.boundary.launcher

import it.unibo.sentinel.boundary.gui.toolkit.Toolkit
import it.unibo.sentinel.boundary.gui.fx.FxToolkit
import it.unibo.sentinel.core.simulation.Simulation
import it.unibo.sentinel.control.Engine
import scala.concurrent.duration.*
import it.unibo.sentinel.control.serialization.FileRepository
import it.unibo.sentinel.core.scenario.Scenario
import it.unibo.sentinel.control.serialization.Repository
import it.unibo.sentinel.core.warehouse.Warehouse
import it.unibo.sentinel.control.serialization.Codec.Validation
import it.unibo.sentinel.core.simulation.SimulationId

/** Application launcher.w
  *
  * Uses a [[Toolkit]] to create and setup a [[Window]], which will display the
  * simulation's [[View]]s.
  */
object Launcher:

  private val toolkit: Toolkit = FxToolkit

  def main(args: Array[String]): Unit =
    for loaded <- loadScenario()
    yield
      val id = SimulationId("sim-1")
      val sim = Simulation.of(id, loaded)
      val engine: Engine = Engine(sim, 1.second)
      val window = toolkit.window
      val panel = toolkit.simulation(engine)
      val statistics = toolkit.statistics()
      window.show(panel)
      window.open()
      engine.observe(panel.render)
      engine.observeCompletion: report =>
        statistics.render(report)
        window.show(statistics)
      engine.start()

  def loadScenario(): Either[Validation, Scenario] =
    import it.unibo.sentinel.control.serialization.JsonSerialization.given

    given warehouseRepo: FileRepository[Warehouse] =
      new FileRepository[Warehouse]
    val scenarioRepo: Repository[String, Scenario] =
      new FileRepository[Scenario]
    warehouseRepo.save(Dataset.warehouse)
    scenarioRepo.save(Dataset.scenario)
    scenarioRepo.load(s"${Dataset.scenario.id}.json")
