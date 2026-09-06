package it.unibo.sentinel.core.mission

import it.unibo.sentinel.core.warehouse.Position
import it.unibo.sentinel.core.item.Item

/** A single atomic physical operation required by a mission.
  */
enum Action:

  /** @param target
    *   The target [[Position]] to reach.
    */
  case Move(to: Position)

  case PickUp(target: Item, at: Position)

  case Drop(target: Item, at: Position)

  def position: Position = this match
    case Move(to)      => to
    case PickUp(_, at) => at
    case Drop(_, at)   => at
