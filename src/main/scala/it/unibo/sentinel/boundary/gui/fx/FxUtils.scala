package it.unibo.sentinel.boundary.gui.fx

import javafx.application.Platform
import monix.eval.Task
import monix.execution.Scheduler
import scala.concurrent.ExecutionContext

/** Utility functions for managing JavaFX/ScalaFX UI execution
  */
object FxUtils:

  /** Default window width
    */
  val defaultWidth: Double = 1400.0

  /** Default window height
    */
  val defaultHeight: Double = 900.0

  /** Runs every submitted task on the JavaFX Application Thread.
    */
  private val fxScheduler: Scheduler =
    Scheduler(
      ExecutionContext.fromExecutor((r: Runnable) => Platform.runLater(r))
    )

  /** Describes an action to be executed on the JavaFX Application Thread.
    *
    * The action is not evaluated until the returned task is run, and the task
    * completes only once the action has been executed, so its completion can be
    * used as a rendering acknowledgement.
    *
    * @param action
    *   the code block to be executed on the JavaFX Application Thread
    */
  def onFx(action: => Unit): Task[Unit] =
    Task(action).executeOn(fxScheduler)
