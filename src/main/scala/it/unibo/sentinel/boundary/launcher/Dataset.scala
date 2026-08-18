package it.unibo.sentinel.boundary.launcher

import it.unibo.sentinel.core.warehouse.Warehouse
import it.unibo.sentinel.core.warehouse.Area
import it.unibo.sentinel.core.warehouse.Tile
import it.unibo.sentinel.core.warehouse.Position

/** Contains default values for a test simulation
  */
trait Dataset:

  /** Width of the warehouse
    */
  protected val width: Int = 50

  /** Height of the warehouse
    */
  protected val height: Int = 30

  /** @return
    *   a [[Warehouse]] of size `width x height` with a ring of non-traversable
    *   tiles all around
    */
  def warehouse: Warehouse =
    val area: Area =
      Area(Position(1, 1), Position(width - 2, height - 2))
    Warehouse
      .empty(width, height)
      .withArea(area):
        Tile.Floor()
