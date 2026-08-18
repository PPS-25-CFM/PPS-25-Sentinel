package it.unibo.sentinel.core.mission

import it.unibo.sentinel.core.warehouse.Position

trait Targeted:
  def target: Position

/** A single atomic physical operation required by a mission.
  */
enum Step extends Targeted:
  /** @param target
    *   The target [[Position]] to reach.
    */
  case Goto(target: Position)
