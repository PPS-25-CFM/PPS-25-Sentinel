package it.unibo.sentinel.boundary.gui.toolkit

import it.unibo.sentinel.core.simulation.StepResult
import it.unibo.sentinel.control.Controller
import it.unibo.sentinel.core.simulation.Statistics.Report
import it.unibo.sentinel.core.warehouse.Warehouse

/** Represents a UI responsible for visualizing a given model
  */
trait View:
  /** The type of the model to render.
    */
  type Model

  /** Loads all the graphics components to visualize the given model
    *
    * @param model
    *   the current state to display
    */
  def render(model: Model): Unit

/** The application's entry view. */
trait MenuView extends View:
  type Model = Unit

  override def render(model: Unit): Unit = ()

  /** Registers the action for the Run Simulation button. */
  def onRunSimulation(action: () => Unit): Unit

/** A view with an action for returning to the menu. */
trait NavigableView extends View:
  /** Registers the action for the Back to Menu button. */
  def onMenu(action: () => Unit): Unit

/** A [[View]] that is able to visualize the [[StepResult]] and interact with
  * the [[Controller]] to control the [[Simulation]].
  */
trait SimulationView extends NavigableView:
  type Model = StepResult

  /** @return
    *   the [[Controller]] that allows to control the [[Simulation]].
    */
  def controller: Controller

/** Displays the report of a completed simulation. */
trait StatisticsView extends NavigableView:
  type Model = Report

/** A [[View]] that is able to visualize the [[Warehouse]] while the user edits
  * it.
  */
trait WarehouseView extends View:
  type Model = Warehouse
