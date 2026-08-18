package it.unibo.sentinel.core.mission

import it.unibo.sentinel.core.warehouse.Position

/** Represents an operational task to be performed within a mission.
  */
enum Task:
  /** @param step
    *   The required atomic [[Step]] to be executed.
    */
  case Act(step: Step)

  /** @return
    *   The target [[Position]] associated with the underlying step.
    */
  def destination: Position = this match
    case Act(step) => step.target
