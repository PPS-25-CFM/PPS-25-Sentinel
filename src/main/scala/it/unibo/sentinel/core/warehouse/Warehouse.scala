package it.unibo.sentinel.core.warehouse

import scala.annotation.internal.requiresCapability

/** */
trait Warehouse:
  /** @return
    *   the width of the warehouse.
    */
  def width: Int

  /** @return
    *   the height of the warehouse.
    */
  def height: Int

  /** @return
    *   the size of the warehouse.
    */
  def size: Int = width * height

object Warehouse:
  /** @param width
    *   the width of the warehouse.
    * @param height
    *   the height of the warehouse.
    * @return
    *   an empty warehouse sized [[width]]x[[height]].
    */
  def empty(w: Int, h: Int): Warehouse =
    require(w > 0 && h > 0)
    new Warehouse:
      override def width: Int = w
      override def height: Int = h
      override def size: Int = w * h