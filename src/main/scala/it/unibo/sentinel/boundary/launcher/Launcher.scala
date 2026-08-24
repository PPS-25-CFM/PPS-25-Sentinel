package it.unibo.sentinel.boundary.launcher

import it.unibo.sentinel.boundary.gui.toolkit.Toolkit
import it.unibo.sentinel.boundary.gui.fx.FxToolkit
import it.unibo.sentinel.core.simulation.Simulation
import it.unibo.sentinel.control.Engine
import scala.concurrent.duration.*
import monix.execution.Scheduler

/** Application launcher.w
  *
  * Uses a [[Toolkit]] to create and setup a [[Window]], which will display the
  * simulation's [[View]]s.
  */
object Launcher extends Dataset:

  private val toolkit: Toolkit = FxToolkit

  def main(args: Array[String]): Unit =
    val window = toolkit.window
    val panel = toolkit.simulation
    window.show(panel)
    window.open()
    val sim = Simulation.of(scenario)
    given Scheduler = Scheduler.singleThread("engine", daemonic = false)
    val engine: Engine = Engine(sim, 1.second)
    engine.observe(panel.render)
    engine.start()
