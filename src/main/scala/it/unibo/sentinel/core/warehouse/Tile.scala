package it.unibo.sentinel.core.warehouse

import it.unibo.sentinel.core.simulation.Tick
import it.unibo.sentinel.core.item.Item

/** Represents a tile in the warehouse.
  */
sealed trait Tile

object Tile:
  /** Validation error generated when creating a [[Tile]].
    */
  enum Validation:
    /** The cost of the tile is negative.
      */
    case NegativeCost(cost: Int)

  trait Walkable extends Tile:
    def cost: Tick

  trait Interactable extends Tile:
    def interactiveOffset(using Adjacency): Seq[Position]


  /** Represents a floor tile.
    */
  case class Floor(cost: Tick = Tick.unit) extends Tile with Walkable

  /** A non-traversable tile that can store one object and can be interacted
    * with from an adjacent traversable tile.
    */
  case class Shelf(item: Item) extends Tile with Interactable:
    override def interactiveOffset(using strategy: Adjacency): Seq[Position] =
      strategy.around(Position(0, 0))

  /** A traversable tile that can store one object and can be interacted with
    * while standing on it.
    */
  case class LoadingBay(cost: Tick = Tick.unit)
      extends Tile
      with Walkable
      with Interactable:
    override def interactiveOffset(using strategy: Adjacency): Seq[Position] =
      Seq(Position(0, 0))