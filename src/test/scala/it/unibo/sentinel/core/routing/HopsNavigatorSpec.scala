package it.unibo.sentinel.core.routing

import it.unibo.sentinel.UnitTest
import it.unibo.sentinel.core.warehouse.{Warehouse, Position, Area, Tile}
import it.unibo.sentinel.core.warehouse.WarehouseId

class HopsNavigatorSpec extends UnitTest with NavigatorBehaviours:

  "A Hops Navigator" when:
    behave like commonNavigator { (warehouse: Warehouse) =>
      Navigator(Metric.Hops)(using warehouse)
    }

  "multiple paths connect two positions" should:
    val from = Position(0, 1)
    val to = Position(2, 1)

    given Warehouse = Warehouse
      .empty(WarehouseId("W"), 3, 3)
      .withArea(
        Area(Position(0, 0), Position(2, 2))
      )(Tile.Floor())

    val navigator = Navigator(Metric.Hops)

    "choose a path minimizing the number of hops" in:
      navigator.distance(from, to).value shouldBe 2

  "computing the destination between multiple destinations" should:
    val from = Position(0, 1)
    val to1 = Position(2, 1)
    val to2 = Position(1, 0)

    given Warehouse = Warehouse
      .empty(WarehouseId("W"), 3, 3)
      .withArea(
        Area(Position(0, 0), Position(2, 2))
      )(Tile.Floor())

    val navigator = Navigator(Metric.Hops)

    "choose the closest destination in terms of hops" in:
      val chosenPath = navigator.path(from, Set(to1, to2)).value
      chosenPath.positions.endsWith(Seq(to1)) shouldBe true
