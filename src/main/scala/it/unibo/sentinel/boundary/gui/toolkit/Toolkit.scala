package it.unibo.sentinel.boundary.gui.toolkit

import it.unibo.sentinel.core.simulation.StepResult

/** Contains a set of components and methods needed to run the application
  */
trait Toolkit:

  type W <: Window
  type V[Model] <: View[Model]

  /** [[Window]] to run the application on
    */
  val window: W { type V[Model] = Toolkit.this.V[Model] }

  /** Creates a [[View]] to display a snapshot of the simulation
    *
    * @return
    *   the simulation [[View]] to display the snapshot
    */
  def simulation: V[StepResult]
