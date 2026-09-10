package it.unibo.sentinel.boundary.gui.toolkit

import monix.eval.Task
import it.unibo.sentinel.core.simulation.StepResult
import it.unibo.sentinel.control.Controller
import it.unibo.sentinel.core.simulation.Statistics.Report

/** Represents a UI responsible for visualizing a given model
  */
trait View:
  /** The type of the model to render.
    */
  type Model

  /** Describes the update of the graphic components visualizing the given
    * model. Nothing is displayed until the returned task is run.
    *
    * @param model
    *   the current state to display.
    */
  def render(model: Model): Task[Unit]

/** A [[View]] that is able to visualize the [[StepResult]] and interact with
  * the [[Controller]] to control the [[Simulation]].
  */
trait SimulationView extends View:
  type Model = StepResult

  /** @return
    *   the [[Controller]] that allows to control the [[Simulation]].
    */
  def controller: Controller

/** Displays the report of a completed simulation. */
trait StatisticsView extends View:
  type Model = Report
