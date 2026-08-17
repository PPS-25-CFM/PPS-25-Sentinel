package it.unibo.sentinel.core.warehouse

import scala.annotation.internal.requiresCapability

/** */
trait Tile

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

  /** @param position
    *   the position to check.
    * @return
    *   whether [[position]] is in bound of the warehouse.
    */
  def inBound(position: Position): Boolean

  /** @param position
    *   the position of the tile to retrieve.
    * @return
    *   an [[Option]] containing the tile at the given position, if any.
    */
  def tileAt(position: Position): Option[Tile] = None

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
    FromLayout(w, h, Map.empty)

  private final case class FromLayout(
      width: Int,
      height: Int,
      layout: Map[Position, Tile]
  ) extends Warehouse:
    override def inBound(position: Position): Boolean = position match
      case Position(x, y) => x >= 0 && x < width && y >= 0 && y < height
