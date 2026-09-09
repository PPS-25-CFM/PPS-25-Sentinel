package it.unibo.sentinel.core.collisions

import it.unibo.sentinel.UnitTest
import it.unibo.sentinel.core.robot.RobotId
import it.unibo.sentinel.core.routing.{Navigator, Path, Step}
import it.unibo.sentinel.core.scenario.Placement
import it.unibo.sentinel.core.simulation.Tick
import it.unibo.sentinel.core.warehouse.{
  Area,
  Position,
  Tile,
  Warehouse,
  WarehouseId
}
import org.mockito.ArgumentMatchers.*
import org.mockito.Mockito.*

class RerouteCollisionHandlerSpec
    extends UnitTest
    with CollisionHandlerBehavior:

  private val alternativePath = Path(
    Step(Position(0, 1), Tick.unit),
    Step(Position(1, 1), Tick.unit)
  )

  private given dummyWarehouse: Warehouse = Warehouse
    .empty(WarehouseId("dummy"), 10, 10)
    .withArea(Area(Position(0, 0), Position(9, 9)))(Tile.Floor(Tick.unit))

  "A CollisionHandler.reroute" when:

    "a new alternative path exists" should:

      val mockNavigator = mock(classOf[Navigator])
      when(mockNavigator.warehouse).thenReturn(dummyWarehouse)
      when(
        mockNavigator.path(
          any[Position],
          any[Set[Position]],
          any[Set[Position]]
        )
      ).thenReturn(Some(alternativePath))
      when(
        mockNavigator.path(
          any[Position],
          any[Position],
          any[Set[Position]]
        )
      ).thenReturn(Some(alternativePath))

      given Navigator = mockNavigator
      val rerouting: CollisionHandler = CollisionHandler.reroute()

      correctCollisionResolver(rerouting)

      "return Action.Reroute with the new path for the yielding robot in indirect collisions" in:
        val r1 = createMovingRobot("R1", Position(1, 1))
        val r2 = createMovingRobot("R2", Position(1, 1))
        val p1 = Placement(r1, Position(0, 0))
        val p2 = Placement(r2, Position(0, 1))
        val actions = rerouting.resolveCollisions(Seq(p1, p2))
        actions(r1.id) shouldBe Action.Move
        actions(r2.id) shouldBe Action.Reroute(alternativePath)

      "make the winner wait for the other robot to reroute and move in direct swap collisions" in:
        val r4 = createMovingRobot("R4", Position(1, 0))
        val r5 = createMovingRobot("R5", Position(0, 0))
        val p4 = Placement(r4, Position(0, 0))
        val p5 = Placement(r5, Position(1, 0))
        val actions = rerouting.resolveCollisions(Seq(p4, p5))
        actions shouldBe Map(
          r4.id -> Action.Wait,
          r5.id -> Action.Reroute(alternativePath)
        )

    "no alternative path exists" should:

      val mockNavigator = mock(classOf[Navigator])
      when(mockNavigator.warehouse).thenReturn(dummyWarehouse)
      when(
        mockNavigator.path(
          any[Position],
          any[Set[Position]],
          any[Set[Position]]
        )
      ).thenReturn(None)
      when(
        mockNavigator.path(
          any[Position],
          any[Position],
          any[Set[Position]]
        )
      ).thenReturn(None)

      given Navigator = mockNavigator
      val fallbackRerouting: CollisionHandler = CollisionHandler.reroute()

      "fallback to Action.Wait for the yielding robot in indirect collisions" in:
        val r1 = createMovingRobot("R1", Position(1, 1))
        val r2 = createMovingRobot("R2", Position(1, 1))
        val p1 = Placement(r1, Position(0, 0))
        val p2 = Placement(r2, Position(0, 1))
        val actions = fallbackRerouting.resolveCollisions(Seq(p1, p2))
        actions(r1.id) shouldBe Action.Move
        actions(r2.id) shouldBe Action.Wait

      "fallback to Action.Wait for both robots in direct swap collisions when no path exists" in:
        val r4 = createMovingRobot("R4", Position(1, 0))
        val r5 = createMovingRobot("R5", Position(0, 0))
        val p4 = Placement(r4, Position(0, 0))
        val p5 = Placement(r5, Position(1, 0))
        val actions = fallbackRerouting.resolveCollisions(Seq(p4, p5))
        actions shouldBe Map(
          r4.id -> Action.Wait,
          r5.id -> Action.Wait
        )
