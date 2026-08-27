package it.unibo.sentinel.core.routing

import it.unibo.sentinel.UnitTest
import it.unibo.sentinel.core.warehouse.{Warehouse, Position, Area, Tile}
import it.unibo.sentinel.core.simulation.Tick

class TimeNavigatorSpec extends UnitTest with NavigatorBehaviours:
  "A TimeNavigator" when:
    behave like commonNavigator { (warehouse: Warehouse) =>
      Navigator(Metric.Time)(using warehouse)
    }

    "multiple paths connect two positions" should:

      val from = Position(0, 1)
      val to = Position(2, 1)

      given Warehouse = Warehouse
        .empty(3, 2)
        .withArea(Area(Position(0, 0), Position(2, 0))):
          Tile.Floor(Tick(1))
        .withArea(Area(Position(0, 1), Position(2, 1))):
          Tile.Floor(Tick(1))
        .withTile(Position(1, 1)):
          Tile.Floor(Tick(10))

      val navigator = Navigator(Metric.Time)

      "choose the path with the lowest traversal time" in:
        navigator.path(from, to).value.positions shouldBe Seq(
          Position(0, 0),
          Position(1, 0),
          Position(2, 0),
          Position(2, 1)
        )
