package it.unibo.sentinel.core.warehouse

import it.unibo.sentinel.UnitTest
import it.unibo.sentinel.core.simulation.Tick

class TileSpec extends UnitTest:
  "A Floor" when:

    "created with no parameters" should:

      "take 1 tick to be crossed" in:
        val floor = Tile.Floor()
        floor.cost shouldBe Tick(1)
