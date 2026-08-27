package it.unibo.sentinel.core.warehouse

import it.unibo.sentinel.core.simulation.Tick

/** Represents a tile in the warehouse.
  */
sealed trait Tile

object Tile:
  /** Represents a floor tile.
    */
  case class Floor(cost: Tick = Tick.unit) extends Tile
