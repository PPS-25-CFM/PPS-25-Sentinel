package it.unibo.sentinel.boundary.launcher

import it.unibo.sentinel.boundary.gui.fx.FxToolkit
import monix.execution.Scheduler.Implicits.global

/** Application launcher.
  *
  * Runs the [[Application]] on the fx [[Toolkit]], keeping the main thread
  * alive until the user closes the window.
  */
object Launcher:
  def main(args: Array[String]): Unit =
    Application(FxToolkit)
      .run()
      .runSyncUnsafe()
