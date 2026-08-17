package it.unibo.sentinel.core.warehouse

import it.unibo.sentinel.UnitTest

trait WarehouseFixture:
  self: UnitTest =>
  val width = 5
  val height = 5
  val w0 = Warehouse.empty(width, height)

class WarehouseSpec extends UnitTest with WarehouseFixture:
  "A Warehouse" when:

    "created" should:

      "throw an IllegalArgumentException if created with wrong dimension" in:
        an[IllegalArgumentException] should be thrownBy Warehouse.empty(0, 0)
