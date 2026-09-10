package it.unibo.sentinel.boundary.gui.toolkit

import it.unibo.sentinel.control.Engine
import it.unibo.sentinel.core.simulation.StepResult

/** A [[View]] that is able to visualize the [[StepResult]] and to command the
  * running [[Simulation]] through [[Engine.Command]]s.
  */
trait SimulationView
    extends View[StepResult],
      Interactive[Engine.Command],
      Dismissable
