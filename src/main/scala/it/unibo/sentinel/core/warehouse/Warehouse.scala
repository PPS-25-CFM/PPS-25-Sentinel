package it.unibo.sentinel.core.warehouse

import it.unibo.sentinel.core.simulation.Tick

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

opaque type WarehouseId = String

object WarehouseId:
  /** @param id
    *   raw string identifier.
    * @return
    *   a [[WarehouseId]] wrapping `id`.
    */
  def apply(id: String): WarehouseId = id

extension (id: WarehouseId)
  /** @return
    *   the identifier as a String
    */
  def value: String = id

/** Abstracts the static structure of a warehouse, which is model as a grid.
  */
trait Warehouse:
  /** @return
    *   the warehouse's identifier.
    */
  def id: WarehouseId

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
      case Some(_: Tile.Walkable) => true
      case _                      => false

  /** @param position
    *   the position to check.
    * @return
    *   whether the tile at `position` is interactable.
    */
  def isInteractable(position: Position): Boolean =
    tileAt(position) match
      case Some(_: Tile.Interactable) => true
      case _                          => false

  /** @param position
    *   the position to check.
    * @return
    *   whether the tile at `position` is a [[Tile.Shelf]].
    */
  def isShelf(position: Position): Boolean =
    tileAt(position) match
      case Some(Tile.Shelf(_)) => true
      case _                   => false

  /** @param position
    *   the position to check.
    * @return
    *   whether the tile at `position` is a [[Tile.LoadingBay]].
    */
  def isLoadingBay(position: Position): Boolean =
    tileAt(position) match
      case Some(Tile.LoadingBay(_)) => true
      case _                        => false

  /** @param position
    *   the position of the tile to retrieve.
    * @return
    *   an [[Option]] containing the tile at the given position, if any.
    */
  def tileAt(position: Position): Option[Tile]

  /** @param position
    *   the position of the tile to retrieve.
    * @return
    *   an [[Option]] containing the traversal cost in [[Tick]] of the tile at
    *   the given.
    */
  def traversalCost(position: Position): Option[Tick] =
    tileAt(position) match
      case Some(tile: Tile.Walkable) => Some(tile.cost)
      case _                         => None

  /** @param position
    *   the position of the interactable tile.
    * @return
    *   traversable, in-bounds positions from which `position` can be interacted
    *   with.
    */
  def interactionPoints(position: Position): Seq[Position] =
    tileAt(position) match
      case Some(tile: Tile.Interactable) =>
        tile.interactiveOffset
          .map(offset => position + offset)
          .filter(interactionPoint =>
            isTraversable(interactionPoint) && inBound(interactionPoint)
          )
      case _ => Seq.empty

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

  /** @return
    *   a [[Seq]] of tuples of [[Position]] and [[Tile]], representing the map
    *   of tiles as a list.
    */
  def tiles: Seq[(Position, Tile)]

  /** @param position
    *   the position whose neighbors are to be retrieved.
    * @return
    *   the neighbors of the given [[position]], according to the given
    *   [[Adjacency]]' strategy.
    */
  def neighbors(position: Position)(using strategy: Adjacency): Seq[Position] =
    strategy.around(position).filter(inBound)

  /** @param position
    *   the position whose traversable neighbours are to be retrieved.
    * @return
    *   in-bounds, traversable neighbours of `position`.
    */
  def traversableNeighbors(position: Position)(using
      strategy: Adjacency
  ): Seq[Position] =
    neighbors(position).filter(isTraversable)

object Warehouse:

  /** Validation error generated when creating a warehouse.
    */
  enum Validation:
    /** The warehouse's size is invalid (`<= 0`).
      */
    case InvalidSize(width: Int, height: Int)

    /** Some tiles are out of bounds.
      */
    case TilesOutOfBounds(positions: Seq[Position], width: Int, height: Int)

  /** @param width
    *   the width of the warehouse.
    * @param height
    *   the height of the warehouse.
    * @return
    *   an empty warehouse sized [[width]]x[[height]].
    */
  def empty(id: WarehouseId, w: Int, h: Int): Warehouse =
    require(w > 0 && h > 0)
    FromLayout(id, w, h, Map.empty)

  private final case class FromLayout(
      id: WarehouseId,
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

    override def tiles: Seq[(Position, Tile)] = layout.toSeq
