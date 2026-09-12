package it.unibo.sentinel.core.routing

import it.unibo.sentinel.UnitTest
import org.mockito.Mockito.*

class ObstaclesSpec extends UnitTest:

  "The Obstacles Metric" when:
    val metric = Metric.Obstacles

    "retrieve the cost for a traversable tile with no nearby obstacles" should:

      "return 1 score" in new MetricFixture:
        when(warehouse.isTraversable(p)).thenReturn(true)
        when(warehouse.traversableNeighbors(p)).thenReturn(neighbors)
        metric.cost(p) shouldBe Some(Score.unit)

    "retrieve the cost for a traversable tile with nearby obstacles" should:

      "return a score equal to 1 plus the number of nearby obstacles" in new MetricFixture:
        when(warehouse.isTraversable(p)).thenReturn(true)
        when(warehouse.traversableNeighbors(p)).thenReturn(traversableNeighbors)

        val expectedScore =
          Score.unit + Score(neighbors.size - traversableNeighbors.size)
        metric.cost(p) shouldBe Some(expectedScore)

    "retrieve the cost for a non traversable tile" should:

      "return no score" in new MetricFixture:
        when(warehouse.isTraversable(p)).thenReturn(false)
        when(warehouse.neighbors(p)).thenReturn(Seq())
        when(warehouse.traversableNeighbors(p)).thenReturn(Seq())
        metric.cost(p) shouldBe None
