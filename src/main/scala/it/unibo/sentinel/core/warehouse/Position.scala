package it.unibo.sentinel.core.warehouse

/** Represents a position in the warehouse.
  */
opaque type Position = (Int, Int)

object Position:
  /** @param x
    *   the x-coordinate of the position.
    * @param y
    *   the y-coordinate of the position.
    * @return
    *   a position with the given coordinates.
    * @throws IllegalArgumentException
    *   if the coordinates are negative.
    */
  def apply(x: Int, y: Int): Position =
    (x, y)

  /** @param p
    *   a position.
    * @return
    *   a tuple containing the x and y coordinates of the position.
    */
  def unapply(p: Position): (Int, Int) = p

  extension (p: Position)
    /** @return
      *   the x-coordinate of the position.
      */
    def x: Int = p._1

    /** @return
      *   the y-coordinate of the position.
      */
    def y: Int = p._2

  /** The ordering of positions is defined by the ordering of their coordinates.
    */
  given Ordering[Position] = Ordering.by { case (x, y) => (x, y) }
