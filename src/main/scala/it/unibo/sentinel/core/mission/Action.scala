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

  /** Picks an [[Item]] from a [[Shelf]].
    *
    * @param target
    *   the expected [[Item]] to pick.
    * @param at
    *   the shelf [[Position]] to pick from.
    */
  case PickUp(target: Item, at: Position)

  /** Drops an [[Item]] onto a [[LoadingBay]].
    *
    * @param target
    *   the [[Item]] to drop.
    * @param at
    *   the loading bay [[Position]] to drop onto.
    */
  case Drop(target: Item, at: Position)

  /** @return
    *   the position associated with this action.
    */
  def position: Position = this match
    case Move(to)      => to
    case PickUp(_, at) => at
    case Drop(_, at)   => at
