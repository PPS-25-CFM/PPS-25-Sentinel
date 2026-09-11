package it.unibo.sentinel.core.collisions

import it.unibo.sentinel.UnitTest
import it.unibo.sentinel.core.item.Item
import it.unibo.sentinel.core.mission.{Mission, MissionId, Priority}
import it.unibo.sentinel.core.robot.{RobotId, value}
import it.unibo.sentinel.core.routing.{Navigator, Path, Step}
import it.unibo.sentinel.core.scenario.Intent
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

  private val r1 = RobotId("R1")
  private val r2 = RobotId("R2")
  private val p0 = Position(0, 0)
  private val p1 = Position(1, 0)
  private val pTarget = Position(1, 1)
  private val alternativePath = Path(
    Step(Position(0, 1), Tick.unit),
    Step(pTarget, Tick.unit)
  )

  private given dummyWarehouse: Warehouse = Warehouse
    .empty(WarehouseId("dummy"), 10, 10)
    .withArea(Area(p0, Position(9, 9)))(Tile.Floor(Tick.unit))

  private def mockNavigator(pathResult: Option[Path]): Navigator =
    val nav = mock(classOf[Navigator])
    when(nav.warehouse).thenReturn(dummyWarehouse)
    when(nav.path(any[Position], any[Set[Position]], any[Set[Position]]))
      .thenReturn(pathResult)
    when(nav.path(any[Position], any[Position], any[Set[Position]]))
      .thenReturn(pathResult)
    nav

  private def relocateIntent(
      id: RobotId,
      from: Position,
      to: Position
  ): Intent =
    Intent(id, from, to, Some(createMission(id.value, to)))

  private def deliverPickIntent(
      id: RobotId,
      from: Position,
      pick: Position,
      drop: Position
  ): Intent =
    Intent(id, from, pick, Some(createDeliverMission(id.value, pick, drop)))

  private def deliverDropIntent(
      id: RobotId,
      currentPos: Position,
      nextPos: Position,
      dropPos: Position
  ): Intent =
    val mission = Mission.deliver(
      MissionId(s"m-drop-${id.value}"),
      Item.Computer,
      currentPos,
      dropPos,
      Tick(10),
      Priority.normal
    )
    Intent(id, currentPos, nextPos, Some(mission))

  "A CollisionHandler.reroute" when:

    "a new alternative path exists" should:

      given Navigator = mockNavigator(Some(alternativePath))
      val rerouting = CollisionHandler.reroute()
      correctCollisionResolver(rerouting)

      "return Action.Reroute with the new path for the yielding robot in indirect collisions" in:
        val i1 = relocateIntent(r1, p0, pTarget)
        val i2 = relocateIntent(r2, Position(0, 1), pTarget)
        val actions = rerouting.resolveCollisions(Seq(i1, i2))
        actions(r1) shouldBe Action.Move
        actions(r2) shouldBe Action.Reroute(alternativePath)

      "make the winner wait for the other robot to reroute and move in direct swap collisions" in:
        val i1 = relocateIntent(r1, p0, p1)
        val i2 = relocateIntent(r2, p1, p0)
        val actions = rerouting.resolveCollisions(Seq(i1, i2))
        actions shouldBe Map(
          r1 -> Action.Wait,
          r2 -> Action.Reroute(alternativePath)
        )

      "reroute yielding robot during pickup phase in indirect collision" in:
        val dropPos = Position(3, 3)
        val i1 = deliverPickIntent(r1, p0, pTarget, dropPos)
        val i2 = deliverPickIntent(r2, Position(0, 1), pTarget, dropPos)
        val actions = rerouting.resolveCollisions(Seq(i1, i2))
        actions(r1) shouldBe Action.Move
        actions(r2) shouldBe Action.Reroute(alternativePath)

      "reroute yielding robot during drop phase in indirect collision" in:
        val dropPos = Position(3, 3)
        val i1 = deliverDropIntent(r1, p0, pTarget, dropPos)
        val i2 = deliverDropIntent(r2, Position(0, 1), pTarget, dropPos)
        val actions = rerouting.resolveCollisions(Seq(i1, i2))
        actions(r1) shouldBe Action.Move
        actions(r2) shouldBe Action.Reroute(alternativePath)

      "reroute yielding robot carrying a deliver mission during direct swap collision (drop phase)" in:
        val dropPos = Position(5, 5)
        val i1 = deliverDropIntent(r1, p0, p1, dropPos)
        val i2 = deliverDropIntent(r2, p1, p0, dropPos)
        val actions = rerouting.resolveCollisions(Seq(i1, i2))
        actions shouldBe Map(
          r1 -> Action.Wait,
          r2 -> Action.Reroute(alternativePath)
        )

    "no alternative path exists" should:

      given Navigator = mockNavigator(None)
      val fallbackRerouting = CollisionHandler.reroute()

      "fallback to Action.Wait for the yielding robot in indirect collisions" in:
        val i1 = relocateIntent(r1, p0, pTarget)
        val i2 = relocateIntent(r2, Position(0, 1), pTarget)
        val actions = fallbackRerouting.resolveCollisions(Seq(i1, i2))
        actions(r1) shouldBe Action.Move
        actions(r2) shouldBe Action.Wait

      "fallback to Action.Wait for both robots in direct swap collisions when no path exists" in:
        val i1 = relocateIntent(r1, p0, p1)
        val i2 = relocateIntent(r2, p1, p0)

        val actions = fallbackRerouting.resolveCollisions(Seq(i1, i2))
        actions shouldBe Map(r1 -> Action.Wait, r2 -> Action.Wait)

      "fallback to Action.Wait for deliver missions (drop phase) when no alternative path exists" in:
        val dropPos = Position(4, 4)
        val i1 = deliverDropIntent(r1, p0, pTarget, dropPos)
        val i2 = deliverDropIntent(r2, Position(0, 1), pTarget, dropPos)

        val actions = fallbackRerouting.resolveCollisions(Seq(i1, i2))
        actions(r1) shouldBe Action.Move
        actions(r2) shouldBe Action.Wait
