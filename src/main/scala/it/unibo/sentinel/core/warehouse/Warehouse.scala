package it.unibo.sentinel.core.warehouse

import scala.annotation.internal.requiresCapability

/** */
trait Warehouse

object Warehouse:
  /** @param width
    *   the width of the warehouse.
    * @param height
    *   the height of the warehouse.
    * @return
    *   an empty warehouse sized [[width]]x[[height]].
    */
  def empty(width: Int, height: Int): Warehouse = 
    require(width > 0 && height > 0)
    ???
