package it.unibo.sentinel.core.routing

import it.unibo.sentinel.UnitTest
import it.unibo.sentinel.core.warehouse.{Position, Warehouse}
import org.mockito.Mockito.*

class ObstaclesSpec extends UnitTest:
  "The Obstacles Metric" when:
    val metric = Metric.Obstacles

    "retrieve the cost for a traversable tile with no nearby obstacles" should:
      given warehouse: Warehouse = mock[Warehouse]()
      val p = mock[Position]()
      val n1 = mock[Position]()
      val n2 = mock[Position]()
      val n3 = mock[Position]()
      val n4 = mock[Position]()
      val neighbors = Seq(n1, n2, n3, n4)
      when(warehouse.isTraversable(p)).thenReturn(true)
      when(warehouse.neighbors(p)).thenReturn(neighbors)
      when(warehouse.traversableNeighbors(p)).thenReturn(neighbors)
      when(warehouse.neighbors(p)).thenReturn(neighbors)

      "return 1 score" in:
        val expectedScore = Score(1)
        metric.cost(p) shouldBe Some(expectedScore)

    "retrieve the cost for a traversable tile with nearby obstacles" should:
      given warehouse: Warehouse = mock[Warehouse]()
      val p = mock[Position]()
      val n1 = mock[Position]()
      val n2 = mock[Position]()
      val n3 = mock[Position]()
      val n4 = mock[Position]()
      val neighbors = Seq(n1, n2, n3, n4)
      val traversableNeighbors = Seq(n1, n2)
      when(warehouse.isTraversable(p)).thenReturn(true)
      when(warehouse.neighbors(p)).thenReturn(neighbors)
      when(warehouse.traversableNeighbors(p)).thenReturn(traversableNeighbors)

      "return a score equal to 1 plus the number of nearby obstacles" in:
        val expectedScore =
          Score(1 + (neighbors.size - traversableNeighbors.size))
        metric.cost(p) shouldBe Some(expectedScore)

    "retrieve the cost for a non traversable tile" should:
      given warehouse: Warehouse = mock[Warehouse]()
      val p = mock[Position]()
      when(warehouse.isTraversable(p)).thenReturn(false)
      when(warehouse.neighbors(p)).thenReturn(Seq())
      when(warehouse.traversableNeighbors(p)).thenReturn(Seq())
      
      "return no score" in:
        metric.cost(p) shouldBe None
