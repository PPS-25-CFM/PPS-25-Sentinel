package it.unibo.sentinel.core.routing

import it.unibo.sentinel.UnitTest
import org.mockito.Mockito.*

class HopsSpec extends UnitTest:
  "The Hops Metric" when:
    val metric = Metric.Hops

    "retrieve the cost for a traversable tile" should:

      "return unit score" in new MetricFixture:
        when(warehouse.isTraversable(p)).thenReturn(true)
        val score = metric.cost(p).value
        score shouldBe Score.unit

    "retrieve the cost for a non traversable tile" should:

      "return no score" in new MetricFixture:
        when(warehouse.isTraversable(p)).thenReturn(false)
        metric.cost(p) shouldBe None
