package it.unibo.sentinel.boundary.gui.toolkit

import monix.eval.Task
import monix.execution.{CancelablePromise, Scheduler}
import monix.reactive.Observable
import monix.reactive.subjects.ConcurrentSubject

/** Represents a UI responsible for rendering a model.
  * @tparam M
  *   the type of the model to render.
  */
trait View[M]:

  /** Renders the given `model` on the UI.
    *
    * @param model
    *   the current state to display.
    * @return
    *   a [[Task]] that completes when the model is rendered.
    */
  def render(model: M): Task[Unit]

/** An interactive UI, which can produce user inputs over time.
  */
trait Interactive[C]:

  given Scheduler = Scheduler.Implicits.global

  private val sink =
    ConcurrentSubject.publish[C]

  protected def emit(command: C): Unit =
    sink.onNext(command)

  /** The user input over time, modeled as an [[Observable]] of commands.
    */
  final def commands: Observable[C] = sink

/** A UI that can be dismissed.
  */
trait Dismissable:

  private val exit = CancelablePromise[Unit]()

  protected def dismiss(): Unit =
    exit.trySuccess(())

  /** @return
    *   a [[Task]] that completes when the UI is dismissed.
    */
  final def dismissed: Task[Unit] = Task.fromCancelablePromise(exit)
