package it.unibo.sentinel.boundary.gui.toolkit

type Snapshot = String

/** Contains a set of components and methods needed to run the application
  */
trait Toolkit:

  /** [[Window]] to run the application on
    */
  val window: Window

  /** Creates a [[View]] to display a snapshot of the simulation
    *
    * @return
    *   the simulation [[View]] to display the snapshot
    */
  def simulation: View[Snapshot]
