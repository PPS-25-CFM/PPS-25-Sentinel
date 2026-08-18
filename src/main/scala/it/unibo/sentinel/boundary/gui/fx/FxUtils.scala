package it.unibo.sentinel.boundary.gui.fx

import scalafx.application.Platform

/** Utility functions for managing JavaFX/ScalaFX UI execution
  */
object FxUtils:

  /** Default window width
    */
  val defaultWidth: Double = 1400.0

  /** Default window height
    */
  val defaultHeight: Double = 900.0

  /** Executes an action on the JavaFx Application Thread.
    *
    * If called from the JavaFX thread, the action runs immediately. Otherwise,
    * it is scheduled to run asynchronously on the JavaFX thread via
    * [[scalafx.application.Platform.runLater Platform.runLater]].
    *
    * @param action
    *   the code block to be executed on the JavaFX Application Thread
    */
  def onFx(action: => Unit): Unit =
    if Platform.isFxApplicationThread then action else Platform.runLater(action)
