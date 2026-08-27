package it.unibo.sentinel.core.routing

import it.unibo.sentinel.UnitTest
import it.unibo.sentinel.core.warehouse.{Warehouse, Position, Area, Tile}

class HopsNavigatorSpec extends UnitTest with NavigatorBehaviours:

  "A Hops Navigator" when:
    behave like commonNavigator { (warehouse: Warehouse) =>
      Navigator(Metric.Hops)(using warehouse)
    }

  "multiple paths connect two positions" should:
    val from = Position(0, 1)
    val to = Position(2, 1)

    given Warehouse = Warehouse
      .empty(3, 3)
      .withArea(
        Area(Position(0, 0), Position(2, 2))
      )(Tile.Floor())

    val navigator = Navigator(Metric.Hops)

    "choose a path minimizing the number of hops" in:
      navigator.distance(from, to).value shouldBe 2
