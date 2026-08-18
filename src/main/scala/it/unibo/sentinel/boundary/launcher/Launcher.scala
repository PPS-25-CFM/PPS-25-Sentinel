package it.unibo.sentinel.boundary.launcher

import it.unibo.sentinel.boundary.gui.toolkit.Toolkit
import it.unibo.sentinel.boundary.gui.fx.FxToolkit

/** Application launcher.
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

    panel.render(warehouse)
