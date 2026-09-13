package it.unibo.sentinel.core.routing

import it.unibo.sentinel.UnitTest
import it.unibo.sentinel.core.simulation.Tick
import org.mockito.Mockito.*

class TimeSpec extends UnitTest:
  "The Time Metric" when:
    val metric = Metric.Time

    "retrieve the cost for a traversable tile" should:

      "return a score equal to the traversal cost" in new MetricFixture:
        val cost = 2
        val expectedScore = Score(cost)
        when(warehouse.traversalCost(p)).thenReturn(Some(Tick(cost)))
        metric.cost(p) shouldBe Some(expectedScore)

    "retrieve the cost for a non traversable tile" should:

      "return no score" in new MetricFixture:
        when(warehouse.traversalCost(p)).thenReturn(None)
        metric.cost(p) shouldBe None
