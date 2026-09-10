package it.unibo.sentinel.boundary.gui.toolkit

import monix.eval.Task

/** Abstraction of a window that opens on the screen to visualize a [[View]].
  *
  * @tparam V
  *   the type of view that the window can show.
  */
trait Window[V]:

  /** Opens the window on the screen.
    * @return
    *   a [[Task]] that completes when the user closes the window.
    */
  def open(): Task[Unit]

  /** Shows a view on the window
    *
    * @param view
    *   the view to display
    * @return
    *   a [[Task]] that completes when the view is displayed.
    */
  def show(view: V): Task[Unit]
