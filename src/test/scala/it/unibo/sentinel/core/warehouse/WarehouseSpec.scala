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
