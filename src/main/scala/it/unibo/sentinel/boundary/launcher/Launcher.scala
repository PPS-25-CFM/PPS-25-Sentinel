package it.unibo.sentinel.boundary.launcher

import it.unibo.sentinel.boundary.gui.fx.FxToolkit
import it.unibo.sentinel.control.serialization.FileRepository
import it.unibo.sentinel.control.serialization.JsonSerialization.given
import it.unibo.sentinel.core.scenario.Scenario
import it.unibo.sentinel.core.warehouse.Warehouse

/** Starts the application with the ScalaFX toolkit. */
object Launcher:
  given FileRepository[Warehouse] = new FileRepository[Warehouse]
  given FileRepository[Scenario] = new FileRepository[Scenario]
  def main(args: Array[String]): Unit =
    new Application(FxToolkit).start()
