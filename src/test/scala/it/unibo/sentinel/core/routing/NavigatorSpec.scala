package it.unibo.sentinel.core.routing

import it.unibo.sentinel.UnitTest
import it.unibo.sentinel.core.warehouse.{Warehouse, Position, Tile}

class NavigatorSpec extends UnitTest:
  "An Hops Navigator" when:

    "two positions are not connected" should:
      given Warehouse = Warehouse.empty(5, 5)
      val navigator = Navigator(metric = Metric.Hops)

      "return no path" in:
        navigator.path(Position(0, 0), Position(4, 4)) shouldBe None

    "two positions are the same" should:
      given Warehouse = Warehouse
        .empty(5, 5)
        .withTile(Position(0, 0))(Tile.Floor())
      val navigator = Navigator(metric = Metric.Hops)

      "return an empty path " in:
        navigator.path(Position(0, 0), Position(0, 0)).value shouldBe Seq.empty
