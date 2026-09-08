package it.unibo.sentinel.core.routing

import it.unibo.sentinel.UnitTest
import it.unibo.sentinel.core.simulation.Tick
import it.unibo.sentinel.core.warehouse.{Position, Warehouse}
import org.mockito.Mockito.*

class TimeSpec extends UnitTest:
  "The Time Metric" when :
    val metric = Metric.Time

    "retrieve the cost for a traversable tile" should :

      "return a score equal to the traversal cost" in :
        given warehouse: Warehouse = mock[Warehouse]()
        val p = mock[Position]()
        val cost = 2
        when(warehouse.traversalCost(p)).thenReturn(Some(Tick(cost)))
        val score = metric.cost(p).value
        score shouldBe Score(cost)

    "retrieve the cost for a non traversable tile" should :

      "return no score" in :
        given warehouse: Warehouse = mock[Warehouse]()
        val p = mock[Position]()
        when(warehouse.isTraversable(p)).thenReturn(false)
        when(warehouse.traversalCost(p)).thenReturn(None)
        metric.cost(p) shouldBe None
