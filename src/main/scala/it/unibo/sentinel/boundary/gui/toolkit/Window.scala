package it.unibo.sentinel.boundary.gui.toolkit

import monix.eval.Task

/** Abstraction of a window that opens on the screen to visualize a [[View]]
  */
trait Window:

  /** The type of [[View]] that the window can show. Depends on the technology
    * chosen to program the UI
    */
  type V <: View

  /** Opens the window on the screen
    */
  def open(): Task[Unit]

  /** Closes the window
    */
  def close(): Task[Unit]

  /** Shows a view on the window
    *
    * @param view
    *   the view to display
    */
  def show(view: V): Task[Unit]
