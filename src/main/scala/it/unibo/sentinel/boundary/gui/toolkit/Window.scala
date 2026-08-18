package it.unibo.sentinel.boundary.gui.toolkit

/** Abstraction of a window that opens on the screen to visualize a [[View]]
  */
trait Window:

  /** The type of [[View]] that the window can show. Depends on the technology
    * chosen to program the UI
    */
  type V[Model] <: View[Model]

  /** Opens the window on the screen
    */
  def open(): Unit

  /** Closes the window
    */
  def close(): Unit

  /** Shows a view on the window
    *
    * @param view
    *   the view to display
    */
  def show[Model](view: V[Model]): Unit
