package it.unibo.sentinel.core.warehouse

/** Represents a tile in the warehouse.
  */
sealed trait Tile

object Tile:
  /** Represents a floor tile.
    */
  case class Floor() extends Tile

/** Defines the strategy for determining adjacent positions.
  */
trait Adjacency:
  /** @param position
    *   the position whose neighbors are to be retrieved.
    * @return
    *   the neighbors of the given [[position]].
    */
  def around(position: Position): Seq[Position]

object Adjacency:
  /** Considers only the four orthogonal positions as adjacent to a given
    * position.
    */
  given orthogonal: Adjacency with
    def around(position: Position): Seq[Position] = position match
      case (x: Int, y: Int) =>
        Seq(
          Position(x + 1, y),
          Position(x, y + 1),
          Position(x - 1, y),
          Position(x, y - 1)
        )

/** The area is defined as the rectangle whose corners are the two given
  * [[Position]]s.
  * @param corner
  *   the first corner of the area.
  * @param opposite
  *   the opposite corner of the area.
  */
case class Area(corner: Position, opposite: Position):
  private val xs =
    math.min(corner.x, opposite.x) to math.max(corner.x, opposite.x)
  private val ys =
    math.min(corner.y, opposite.y) to math.max(corner.y, opposite.y)

  /** @return
    *   the sequence of [[Position]]s contained in the area, including the
    *   corners.
    */
  def positions: Seq[Position] = for
    x <- xs
    y <- ys
  yield Position(x, y)

/** Abstracts the static structure of a warehouse, which is model as a grid.
  */
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
    *   the position to check.
    * @return
    *   whether [[position]] is traversable.
    */
  def isTraversable(position: Position): Boolean =
    tileAt(position) match
      case Some(Tile.Floor()) => true
      case _                  => false

  /** @param position
    *   the position of the tile to retrieve.
    * @return
    *   an [[Option]] containing the tile at the given position, if any.
    */
  def tileAt(position: Position): Option[Tile]

  /** @param position
    *   the position of the tile to add.
    * @param tile
    *   the tile to add.
    * @return
    *   a new warehouse with the given tile at the given [[position]].
    */
  def withTile(position: Position)(tile: Tile): Warehouse

  /** @param area
    *   the area to fill with the given tile.
    * @param tile
    *   the tile to add.
    * @return
    *   a new warehouse with the given tile at every position of the given
    *   [[area]].
    */
  def withArea(area: Area)(tile: Tile): Warehouse =
    area.positions.foldLeft(this):
      _.withTile(_)(tile)

  /** @param position
    *   the position of the tile to remove.
    * @return
    *   a new warehouse without the tile at the given [[position]].
    */
  def withoutTile(position: Position): Warehouse

  /** @param position
    *   the position whose neighbors are to be retrieved.
    * @return
    *   the neighbors of the given [[position]], according to the given
    *   [[Adjacency]]' strategy.
    */
  def neighbors(position: Position)(using strategy: Adjacency): Seq[Position] =
    strategy.around(position).filter(inBound)

  def traversableNeighbors(position: Position)(using
      strategy: Adjacency
  ): Seq[Position] =
    neighbors(position).filter(isTraversable)

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

    override def tileAt(position: Position): Option[Tile] = layout.get(position)

    override def withTile(position: Position)(tile: Tile): Warehouse =
      copy(layout = layout + (position -> tile))

    override def withoutTile(position: Position): Warehouse =
      copy(layout = layout - position)
