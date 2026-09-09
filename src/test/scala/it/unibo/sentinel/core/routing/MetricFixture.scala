package it.unibo.sentinel.core.routing
import it.unibo.sentinel.core.warehouse.{Position, Warehouse}
import org.mockito.Mockito.*

trait MetricFixture:
  given warehouse: Warehouse = mock[Warehouse]()
  val p = mock[Position]()
  val n1 = mock[Position]()
  val n2 = mock[Position]()
  val n3 = mock[Position]()
  val n4 = mock[Position]()
  val neighbors = Seq(n1, n2, n3, n4)
  val traversableNeighbors = Seq(n1, n2)
  when(warehouse.neighbors(p)).thenReturn(neighbors)
