package it.unibo.sentinel.core.warehouse

import it.unibo.sentinel.UnitTest
import it.unibo.sentinel.core.simulation.Tick

class TileSpec extends UnitTest:
  "A Floor" when:

    "created with no parameters" should:

      "have 1 time cost" in:
        val floor = Tile.Floor()
        floor.cost shouldBe Tick(1)
