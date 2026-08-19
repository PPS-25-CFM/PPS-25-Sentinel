package it.unibo.sentinel.core.mission

import it.unibo.sentinel.core.warehouse.Position

/** A single atomic physical operation required by a mission.
  */
private[mission] enum Step:

  /** @param target
    *   The target [[Position]] to reach.
    */
  case Move(target: Position)

  /** @return
    *   The target [[Position]] associated with this step, if applicable.
    */
  def targetPosition: Position = this match
    case Move(pos) => pos
