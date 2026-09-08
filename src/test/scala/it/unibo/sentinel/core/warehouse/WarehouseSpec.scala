package it.unibo.sentinel.core.warehouse

import it.unibo.sentinel.UnitTest
import it.unibo.sentinel.core.item.Item
import it.unibo.sentinel.core.simulation.Tick

trait WarehouseFixture:
  self: UnitTest =>
  val id = WarehouseId("W1")
  val width = 5
  val height = 5
  val w0 = Warehouse.empty(id, width, height)
  val gridPositions = for
    x <- 0 until width
    y <- 0 until height
  yield Position(x, y)
  val outerPositions = Seq(
    Position(width, 0),
    Position(0, height),
    Position(width, height),
    Position(width, height - 1),
    Position(width - 1, height)
  )

class WarehouseSpec extends UnitTest with WarehouseFixture:
  "A Warehouse" when:

    "created" should:

      "throw an IllegalArgumentException if created with wrong dimension" in:
        an[IllegalArgumentException] should be thrownBy Warehouse.empty(
          id,
          0,
          0
        )

      "have the correct ID" in:
        w0.id shouldBe id

      "keep the requested dimensions" in:
        w0.width shouldBe width
        w0.height shouldBe height

      "have a size equal to width * height" in:
        w0.size shouldBe width * height

      "contains no tiles" in:
        forAll(gridPositions):
          w0.tileAt(_) shouldBe None

      "consider in bound every position of the grid" in:
        forAll(gridPositions):
          w0.inBound(_) shouldBe true

      "consider out of bound every position outside the grid" in:
        forAll(outerPositions):
          w0.inBound(_) shouldBe false

    "a floor is added" should:

      "expose that floor at the given position" in:
        val position = Position(1, 1)
        val w1 = w0.withTile(position)(Tile.Floor())
        w1.tileAt(position) shouldBe Some(Tile.Floor())

      "expose its traversal cost at the given position" in:
        val position = Position(1, 1)
        val w1 = w0.withTile(position)(Tile.Floor(Tick(5)))
        w1.traversalCost(position) shouldBe Some(Tick(5))

    "a floor is removed" should:

      "no longer expose it" in:
        val position = Position(1, 1)
        val w1 = w0.withTile(position)(Tile.Floor())
        val w2 = w1.withoutTile(position)
        w2.tileAt(position) shouldBe None

    "an area is filled" should:

      "expose a tile in each position of the area" in:
        val p1 = Position(1, 1)
        val p2 = Position(2, 2)
        val area = Area(p1, p2)
        val w1 = w0.withArea(area)(Tile.Floor())
        forAll(area.positions):
          w1.tileAt(_) shouldBe Some(Tile.Floor())

      "leave the positions outside the area empty" in:
        val p1 = Position(1, 1)
        val p2 = Position(2, 2)
        val area = Area(p1, p2)
        val w1 = w0.withArea(area)(Tile.Floor())
        forAll(gridPositions.filterNot(area.positions.contains)):
          w1.tileAt(_) shouldBe None

    "asked for the neighbors of a position" should:

      "return the four orthogonal ones, for an inner position" in:
        val inner = Position(2, 2)
        w0.neighbors(inner) should contain theSameElementsAs
          Seq(
            Position(2, 1),
            Position(2, 3),
            Position(1, 2),
            Position(3, 2)
          )

      "never include the position itself" in:
        forAll(gridPositions): p =>
          w0.neighbors(p) should not contain p

      "drop the ones falling outside the grid, on the borders" in:
        w0.neighbors(Position(0, 0)) should contain theSameElementsAs
          Seq(Position(1, 0), Position(0, 1))
        w0.neighbors(Position(width - 1, height - 1)) should
          contain theSameElementsAs
          Seq(Position(width - 2, height - 1), Position(width - 1, height - 2))

    "asked whether a position is traversable" should:

      "answer positively on a floor tile" in:
        val position = Position(1, 1)
        val w1 = w0.withTile(position)(Tile.Floor())
        w1.isTraversable(position) shouldBe true

      "answer negatively on an empty position" in:
        w0.isTraversable(Position(0, 0)) shouldBe false

    "asked for the traversable neighbors of a position" should:

      "return only the neighbors holding a floor tile" in:
        val p1 = Position(1, 1)
        val p2 = Position(2, 2)
        val area = Area(p1, p2)
        val w1 = w0.withArea(area)(Tile.Floor())
        w1.traversableNeighbors(p2) should contain theSameElementsAs
          Seq(Position(2, 1), Position(1, 2))

      "be empty when no neighbor holds a tile" in:
        val position = Position(1, 1)
        w0.traversableNeighbors(position) shouldBe empty

    "a shelf is added" should:

      "expose that shelf at the given position" in:
        val pos = Position(1, 1)
        val w1 = w0.withTile(pos)(Tile.Shelf(Item.Computer))
        w1.tileAt(pos) shouldBe Some(Tile.Shelf(Item.Computer))

      "not be traversable" in:
        val pos = Position(1, 1)
        val w1 = w0.withTile(pos)(Tile.Shelf(Item.Computer))
        w1.isTraversable(pos) shouldBe false
        w1.traversalCost(pos) shouldBe None

      "be recognized as shelf, interactable but not loading bay" in:
        val pos = Position(2, 2)
        val w1 = w0.withTile(pos)(Tile.Shelf(Item.Table))
        w1.isShelf(pos) shouldBe true
        w1.isLoadingBay(pos) shouldBe false
        w1.isInteractable(pos) shouldBe true

    "a loading bay is added" should:

      "expose that loading bay at the given position" in:
        val pos = Position(1, 2)
        val w1 = w0.withTile(pos)(Tile.LoadingBay())
        w1.tileAt(pos) shouldBe Some(Tile.LoadingBay())

      "be both traversable and interactable" in:
        val pos = Position(1, 2)
        val w1 = w0.withTile(pos)(Tile.LoadingBay(Tick(3)))
        w1.isTraversable(pos) shouldBe true
        w1.isInteractable(pos) shouldBe true
        w1.isLoadingBay(pos) shouldBe true
        w1.isShelf(pos) shouldBe false
        w1.traversalCost(pos) shouldBe Some(Tick(3))

      "have default cost Tick.unit when not specified" in:
        val pos = Position(0, 0)
        val w1 = w0.withTile(pos)(Tile.LoadingBay())
        w1.traversalCost(pos) shouldBe Some(Tick.unit)
        w1.isTraversable(pos) shouldBe true

    "asked whether a position is shelf or loading bay" should:

      "answer negatively on an empty position" in:
        w0.isShelf(Position(0, 0)) shouldBe false
        w0.isLoadingBay(Position(0, 0)) shouldBe false
        w0.isInteractable(Position(0, 0)) shouldBe false

      "answer negatively on a floor tile" in:
        val pos = Position(1, 1)
        val w1 = w0.withTile(pos)(Tile.Floor())
        w1.isShelf(pos) shouldBe false
        w1.isLoadingBay(pos) shouldBe false
        w1.isInteractable(pos) shouldBe false

      "answer positively on matching tiles" in:
        val shelfPos = Position(1, 1)
        val bayPos = Position(2, 2)
        val w1 = w0
          .withTile(shelfPos)(Tile.Shelf(Item.Fridge))
          .withTile(bayPos)(Tile.LoadingBay())
        w1.isShelf(shelfPos) shouldBe true
        w1.isLoadingBay(bayPos) shouldBe true
