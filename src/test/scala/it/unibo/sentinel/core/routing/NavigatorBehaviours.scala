package it.unibo.sentinel.core.routing

import it.unibo.sentinel.UnitTest
import it.unibo.sentinel.core.warehouse.{
  Warehouse,
  Position,
  Tile,
  Area,
  WarehouseId
}

trait NavigatorBehaviours:
  self: UnitTest =>

  def commonNavigator(build: Warehouse => Navigator): Unit =

    "the destination is unreachable" should:
      val warehouse = Warehouse.empty(WarehouseId("W"), 5, 5)
      val navigator = build(warehouse)

      "return no path" in:
        navigator.path(Position(0, 0), Position(4, 4)) shouldBe None

    "origin and destination coincide" should:
      val warehouse = Warehouse
        .empty(WarehouseId("W"), 5, 5)
        .withTile(Position(0, 0))(Tile.Floor())
      val navigator = build(warehouse)

      "return an empty path" in:
        navigator
          .path(Position(0, 0), Position(0, 0))
          .value shouldBe Path.empty

    "only one path exists" should:
      val warehouse = Warehouse
        .empty(WarehouseId("W"), 5, 5)
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

    "calculating a path with avoided positions" should:
      val warehouse = Warehouse
        .empty(WarehouseId("W"), 5, 5)
        .withArea(Area(Position(1, 1), Position(3, 3)))(Tile.Floor())
      val navigator = build(warehouse)

      "reach the destination without crossing any avoided position" in:
        val avoiding = Set(Position(1, 2), Position(2, 2))
        val path = navigator
          .path(Position(2, 1), Position(2, 3), avoiding)
          .value

        path.destination shouldBe Some(Position(2, 3))
        path.positions.toSet.intersect(avoiding) shouldBe empty

      "return no path when avoided positions block every route" in:
        navigator
          .path(
            Position(2, 1),
            Position(2, 3),
            Set(Position(1, 2), Position(2, 2), Position(3, 2))
          ) shouldBe None

      "return no path when the destination is avoided" in:
        navigator
          .path(
            Position(2, 1),
            Position(2, 3),
            Set(Position(2, 3))
          ) shouldBe None

      "reach an allowed destination without crossing avoided positions" in:
        val avoiding = Set(Position(1, 2), Position(2, 2))
        val path = navigator
          .path(
            Position(2, 1),
            Set(Position(2, 2), Position(2, 3)),
            avoiding
          )
          .value

        path.destination shouldBe Some(Position(2, 3))
        path.positions.toSet.intersect(avoiding) shouldBe empty
