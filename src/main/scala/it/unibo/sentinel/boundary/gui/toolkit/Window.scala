package it.unibo.sentinel.boundary.gui.toolkit

/** Abstraction of a window that opens on the screen to visualize a [[View]]
  */
trait Window:

  /** The type of [[View]] that the window can show. Depends on the technology
    * chosen to program the UI
    */
  type V <: View

  /** Opens the window on the screen
    */
  def open(): Unit

  /** Closes the window
    */
  def close(): Unit

  /** Registers cleanup for both user and programmatic window closure. */
  def onClose(action: () => Unit): Unit

  /** Shows a view on the window
    *
    * @param view
    *   the view to display
    */
  def show(view: V): Unit

  /** Selects a JSON file, or returns None when the dialog is canceled. Must be
    * called on the UI thread.
    */
  def chooseJsonFile(): Option[os.Path]

  /** Displays a loading error. Must be called on the UI thread. */
  def showError(message: String): Unit
