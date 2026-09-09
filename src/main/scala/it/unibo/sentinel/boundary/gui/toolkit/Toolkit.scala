package it.unibo.sentinel.boundary.gui.toolkit

import it.unibo.sentinel.control.Controller

/** Contains a set of components and methods needed to run the application
  */
trait Toolkit:

  type W <: Window
  type V <: View

  /** [[Window]] to run the application on
    */
  val window: W { type V = Toolkit.this.V }

  /** Executes immediately on the UI thread, or queues the action when called
    * from another thread.
    */
  def execute(action: => Unit): Unit

  /** Creates the application's main menu. */
  def menu(): V & MenuView

  /** Creates a [[View]] to display a snapshot of the simulation
    *
    * @return
    *   the simulation [[View]] to display the snapshot
    */
  def simulation(controller: Controller): V & SimulationView

  /** Creates a view for the final simulation report. */
  def statistics(): V & StatisticsView
