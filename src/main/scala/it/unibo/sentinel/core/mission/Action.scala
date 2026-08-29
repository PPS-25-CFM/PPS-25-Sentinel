package it.unibo.sentinel.core.mission

import it.unibo.sentinel.core.warehouse.Position

/** A single atomic physical operation required by a mission.
  */
enum Action:

  /** @param target
    *   The target [[Position]] to reach.
    */
  case Move(target: Position)

  def position: Position = this match
    case Move(p) => p
