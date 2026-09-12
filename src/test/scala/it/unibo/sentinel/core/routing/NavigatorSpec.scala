package it.unibo.sentinel.core.routing

import it.unibo.sentinel.UnitTest
import it.unibo.sentinel.core.simulation.Tick
import it.unibo.sentinel.core.warehouse.{Position, Warehouse}
import org.mockito.Mockito.*
import org.mockito.ArgumentMatchers.*

class NavigatorSpec extends UnitTest:
  "A Navigator" when:
    val metric = mock[Metric]()
    when(metric.cost(any[Position])(using any[Warehouse]))
      .thenReturn(Some(Score.unit))

    "the destination is unreachable" should:
      given warehouse: Warehouse = mock[Warehouse]()
      val p1 = mock[Position]()
      val p2 = mock[Position]()
      when(warehouse.traversableNeighbors(p1)).thenReturn(Seq.empty)

      "return no path" in:
        Navigator(metric).path(p1, p2) shouldBe None

    "origin and destination are the same" should:
      given warehouse: Warehouse = mock[Warehouse]()
      val p1 = mock[Position]()

      "return an empty path" in:
        Navigator(metric).path(p1, p1).value shouldBe Path.empty

    "only one path exists" should:
      given warehouse: Warehouse = mock[Warehouse]()
      val p1 = mock[Position]()
      val p2 = mock[Position]()
      val p3 = mock[Position]()
      when(warehouse.traversalCost(any[Position])).thenReturn(Some(Tick(1)))
      when(warehouse.traversableNeighbors(p1)).thenReturn(Seq(p2))
      when(warehouse.traversableNeighbors(p2)).thenReturn(Seq(p3))

      "return that path" in:
        val expectedPath = Seq(p2, p3)
        Navigator(metric).path(p1, p3).value.positions shouldBe expectedPath

    "multiple paths connect two positions" should:
      given warehouse: Warehouse = mock[Warehouse]()
      val p1 = mock[Position]()
      val p2 = mock[Position]()
      val p3 = mock[Position]()
      val p4 = mock[Position]()
      when(warehouse.traversalCost(any[Position])).thenReturn(Some(Tick(1)))
      when(warehouse.traversableNeighbors(p1)).thenReturn(Seq(p2))
      when(warehouse.traversableNeighbors(p2)).thenReturn(Seq(p3, p4))
      when(warehouse.traversableNeighbors(p3)).thenReturn(Seq(p2, p4))

      "choose a path which minimizes the given metric" in:
        val path = Navigator(metric).path(p1, p4).value
        path.positions shouldBe Seq(p2, p4)

    "multiple destinations are passed" should:
      given warehouse: Warehouse = mock[Warehouse]()
      val p1 = mock[Position]()
      val p2 = mock[Position]()
      val p3 = mock[Position]()
      when(warehouse.traversalCost(any[Position])).thenReturn(Some(Tick(1)))
      when(warehouse.traversableNeighbors(p1)).thenReturn(Seq(p2))
      when(warehouse.traversableNeighbors(p2)).thenReturn(Seq(p3))

      "choose the closest destination in terms of the given metric" in:
        val path = Navigator(metric).path(p1, Set(p2, p3)).value
        path.positions.endsWith(Seq(p2)) shouldBe true

    "calculating a path with avoiding positions" should:
      given warehouse: Warehouse = mock[Warehouse]()
      val p1 = mock[Position]()
      val p2 = mock[Position]()
      val p3 = mock[Position]()
      val p4 = mock[Position]()
      when(warehouse.traversalCost(any[Position])).thenReturn(Some(Tick(1)))
      when(warehouse.traversableNeighbors(p1)).thenReturn(Seq(p2, p3))
      when(warehouse.traversableNeighbors(p2)).thenReturn(Seq(p4))
      when(warehouse.traversableNeighbors(p3)).thenReturn(Seq(p2, p4))

      "return a path that avoids the given positions" in:
        val avoiding = Set(p2)
        val path = Navigator(metric).path(p1, p4, avoiding).value
        path.positions shouldBe Seq(p3, p4)

      "return no path when the avoiding positions block all paths" in:
        val avoiding = Set(p2, p3)
        Navigator(metric).path(p1, p4, avoiding) shouldBe None

      "return no path when the avoiding positions include the destination" in:
        val avoiding = Set(p4)
        Navigator(metric).path(p1, p4, avoiding) shouldBe None
