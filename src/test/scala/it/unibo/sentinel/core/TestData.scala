package it.unibo.sentinel.core

import it.unibo.sentinel.UnitTest
import it.unibo.sentinel.core.scenario.Scenario
import it.unibo.sentinel.core.warehouse.{
  Area,
  Position,
  Tile,
  Warehouse,
  WarehouseId
}

/** A mix in that contains a simple dataset for testing.
  */
trait TestData:
  self: UnitTest =>
  // Warehouse
  val width = 5
  val height = 5
  val room = Area(Position(1, 1), Position(width - 2, height - 2))
  val warehouse: Warehouse =
    Warehouse
      .empty(WarehouseId("W"), width, height)
      .withArea(room)(Tile.Floor())
  // Scenario
  val emptyScenario: Scenario = Scenario.in(warehouse)
