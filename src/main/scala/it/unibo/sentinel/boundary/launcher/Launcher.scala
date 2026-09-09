package it.unibo.sentinel.boundary.launcher

import it.unibo.sentinel.boundary.gui.fx.FxToolkit

/** Starts the application with the ScalaFX toolkit. */
object Launcher:
  def main(args: Array[String]): Unit =
    new Application(FxToolkit).start()
