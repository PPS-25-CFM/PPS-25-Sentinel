package it.unibo.sentinel.core.warehouse

import it.unibo.sentinel.UnitTest

trait WarehouseFixture:
  self: UnitTest =>
  val width = 5
  val height = 5
  val w0 = Warehouse.empty(width, height)
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
        an[IllegalArgumentException] should be thrownBy Warehouse.empty(0, 0)

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

    "a tile is added" should:
      "expose that tile at the given position" in:
        val position = Position(1, 1)
        val w1 = w0.withTile(position)(Tile.Floor())
        w1.tileAt(position) shouldBe Some(Tile.Floor())
