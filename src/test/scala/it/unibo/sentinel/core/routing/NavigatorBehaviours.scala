package it.unibo.sentinel.core.routing

import it.unibo.sentinel.UnitTest
import it.unibo.sentinel.core.warehouse.{Warehouse, Position, Tile, Area}

trait NavigatorBehaviours:
  self: UnitTest =>

  def commonNavigator(build: Warehouse => Navigator): Unit =

    "the destination is unreachable" should:
      val warehouse = Warehouse.empty(5, 5)
      val navigator = build(warehouse)

      "return no path" in:
        navigator.path(Position(0, 0), Position(4, 4)) shouldBe None

    "origin and destination coincide" should:
      val warehouse = Warehouse
        .empty(5, 5)
        .withTile(Position(0, 0))(Tile.Floor())
      val navigator = build(warehouse)

      "return an empty path" in:
        navigator
          .path(Position(0, 0), Position(0, 0))
          .value shouldBe Path.empty

    "only one path exists" should:
      val warehouse = Warehouse
        .empty(5, 5)
        .withArea(Area(Position(0, 0), Position(0, 2)))(Tile.Floor())
      val navigator = build(warehouse)

      "return that path" in:
        navigator
          .path(Position(0, 0), Position(0, 2))
          .value
          .positions shouldBe Seq(
          Position(0, 1),
          Position(0, 2)
        )
