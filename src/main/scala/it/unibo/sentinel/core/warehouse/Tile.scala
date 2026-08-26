package it.unibo.sentinel.core.warehouse

/** Represents a tile in the warehouse.
  */
sealed trait Tile

object Tile:
  /** Represents a floor tile.
    */
  case class Floor() extends Tile
