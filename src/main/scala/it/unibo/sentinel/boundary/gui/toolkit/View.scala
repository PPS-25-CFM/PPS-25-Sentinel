package it.unibo.sentinel.boundary.gui.toolkit

import monix.eval.Task
import monix.reactive.Observable

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
  /** The user input over time, modeled as an [[Observable]] of commands.
    */
  def commands: Observable[C]

/** A UI that can be dismissed.
  */
trait Dismissable:
  /** @return
    *   a [[Task]] that completes when the UI is dismissed.
    */
  def dismissed: Task[Unit]
