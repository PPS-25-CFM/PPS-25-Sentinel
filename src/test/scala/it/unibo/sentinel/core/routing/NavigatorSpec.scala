package it.unibo.sentinel.core.routing

import it.unibo.sentinel.UnitTest
import it.unibo.sentinel.core.warehouse.{Warehouse, Position}

class NavigatorSpec extends UnitTest:
  "An Hops Navigator" when:

    "two positions are not connected" should:
      given Warehouse = Warehouse.empty(5, 5)
      val navigator = Navigator(metric = Metric.Hops)

      "return no path" in:
        navigator.path(Position(0, 0), Position(4, 4)) shouldBe None
