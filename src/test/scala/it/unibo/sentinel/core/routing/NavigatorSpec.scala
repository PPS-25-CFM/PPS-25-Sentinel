package it.unibo.sentinel.core.routing

import it.unibo.sentinel.UnitTest
import it.unibo.sentinel.core.warehouse.{Warehouse, Position, Tile, Area}

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

    "only a path exists between two positions" should:
      given Warehouse = Warehouse
        .empty(5, 5)
        .withArea(Area(Position(0, 0), Position(0, 2)))(Tile.Floor())
      val navigator = Navigator(metric = Metric.Hops)

      "return such path" in:
        navigator.path(Position(0, 0), Position(0, 2)).value shouldBe Seq(
          Position(0, 1),
          Position(0, 2)
        )

    "multiple paths connect two positions" should:
      val from = Position(0, 1)
      val to = Position(2, 1)
      given Warehouse =
        Warehouse
          .empty(3, 3)
          .withArea(Area(Position(0, 0), Position(2, 2)))(Tile.Floor())
      val navigator = Navigator(Metric.Hops)

      "choose a path which minimizes hops counter" in:
        navigator.distance(from, to).value shouldBe 2
