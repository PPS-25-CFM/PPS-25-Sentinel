package it.unibo.sentinel.core.routing

import it.unibo.sentinel.UnitTest
import it.unibo.sentinel.core.warehouse.{Position, Warehouse}
import org.mockito.Mockito.*

class HopsSpec extends UnitTest:
  "The Hops Metric" when:
    val metric = Metric.Hops

    "retrieve the cost for a traversable tile" should:

      "return 1 score" in:
        given warehouse: Warehouse = mock[Warehouse]()
        val p = mock[Position]()
        when(warehouse.isTraversable(p)).thenReturn(true)
        val score = metric.cost(p).value
        score shouldBe Score(1)

    "retrieve the cost for a non traversable tile" should:

      "return no score" in:
        given warehouse: Warehouse = mock[Warehouse]()
        val p = mock[Position]()
        when(warehouse.isTraversable(p)).thenReturn(false)
        metric.cost(p) shouldBe None