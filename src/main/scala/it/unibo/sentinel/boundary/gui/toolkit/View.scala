package it.unibo.sentinel.boundary.gui.toolkit

import it.unibo.sentinel.core.simulation.StepResult
import it.unibo.sentinel.control.Controller

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

/** A [[View]] that is able to visualize the [[StepResult]] and interact with
  * the [[Controller]] to control the [[Simulation]].
  */
trait SimulationView extends View:
  type Model = StepResult

  /** @return
    *   the [[Controller]] that allows to control the [[Simulation]].
    */
  def controller: Controller
