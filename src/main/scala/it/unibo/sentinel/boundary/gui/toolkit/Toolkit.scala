package it.unibo.sentinel.boundary.gui.toolkit

import monix.eval.Task

/** Contains a set of components and methods needed to run the application
  */
trait Toolkit:

  /** The type of the views that [[Window]] can show.
    */
  type V

  /** [[Window]] to run the application on
    */
  val window: Window[V]

  /** The menu of the application.
    */
  def menu: V & Menu

  /** Creates a [[View]] to display a snapshot of the simulation
    *
    * @return
    *   the [[SimulationView]].
    */
  def simulation: V & SimulationView

  /** Creates a view for the final simulation report. */
  def statistics: V & StatisticsView

  /** Shuts down the application.
    *
    * @return
    *   a [[Task]] that completes when the application is shut down
    */
  def shutdown(): Task[Unit]
