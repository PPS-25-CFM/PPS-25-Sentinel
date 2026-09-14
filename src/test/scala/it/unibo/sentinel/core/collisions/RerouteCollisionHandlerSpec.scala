package it.unibo.sentinel.core.collisions

import it.unibo.sentinel.UnitTest
import it.unibo.sentinel.core.item.Item
import it.unibo.sentinel.core.mission.{Mission, MissionId, Priority}
import it.unibo.sentinel.core.robot.{RobotId, value}
import it.unibo.sentinel.core.routing.{Navigator, Path, Step}
import it.unibo.sentinel.core.scenario.Intent
import it.unibo.sentinel.core.simulation.Tick
import it.unibo.sentinel.core.warehouse.*
import org.mockito.ArgumentMatchers.*
import org.mockito.Mockito.*

class RerouteCollisionHandlerSpec
    extends UnitTest
    with CollisionHandlerBehavior:

  private val p0 = Position(0, 0)
  private val p1 = Position(1, 0)
  private val p2 = Position(2, 0)
  private val dropPos = Position(3, 3)
  private val alternativePath = Path(
    Step(Position(0, 1), Tick.unit),
    Step(Position(1, 1), Tick.unit)
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

  private def deliveryMission(id: String, pick: Position, drop: Position) =
    Mission.deliver(
      MissionId(s"m-drop-${id}"),
      Item.Computer,
      pick,
      drop,
      Tick(10),
      Priority.normal
    )

  private def pickIntent(
      id: RobotId,
      from: Position,
      pick: Position,
      drop: Position
  ): Intent =
    Intent(id, from, pick, Some(deliveryMission(id.value, pick, drop)))

  private def dropIntent(
      id: RobotId,
      from: Position,
      to: Position,
      drop: Position
  ): Intent =
    Intent(id, from, to, Some(deliveryMission(id.value, from, drop)))

  "A CollisionHandler with reroute" when:

    "a new alternative path exists" should:

      given Navigator = mockNavigator(Some(alternativePath))
      val rerouting = CollisionHandler.reroute()
      baseHandler(rerouting)

      "reroute the yielding robot and let the other move in indirect collisions" in:
        val i1 = moveIntent(r1, p0, p1)
        val i2 = moveIntent(r2, p2, p1)
        val actions = rerouting.resolveCollisions(Seq(i1, i2))
        actions(r1) shouldBe Action.Move
        actions(r2) shouldBe Action.Reroute(alternativePath)

      "reroute yielding robot during pickup phase in indirect collision" in:
        val i1 = pickIntent(r1, p0, p1, dropPos)
        val i2 = pickIntent(r2, p2, p1, dropPos)
        val actions = rerouting.resolveCollisions(Seq(i1, i2))
        actions(r1) shouldBe Action.Move
        actions(r2) shouldBe Action.Reroute(alternativePath)

      "reroute yielding robot during drop phase in indirect collision" in:
        val i1 = dropIntent(r1, p0, p1, dropPos)
        val i2 = dropIntent(r2, p2, p1, dropPos)
        val actions = rerouting.resolveCollisions(Seq(i1, i2))
        actions(r1) shouldBe Action.Move
        actions(r2) shouldBe Action.Reroute(alternativePath)

      "reroute yielding robot and make the other wait in direct collision" in:
        val i1 = moveIntent(r1, p0, p1)
        val i2 = moveIntent(r2, p1, p0)
        val actions = rerouting.resolveCollisions(Seq(i1, i2))
        actions shouldBe Map(
          r1 -> Action.Wait,
          r2 -> Action.Reroute(alternativePath)
        )

    "no alternative path exists" should:

      given Navigator = mockNavigator(None)
      val rerouting = CollisionHandler.reroute()

      "block the yielding robot and let the other move in indirect collisions" in:
        val i1 = moveIntent(r1, Position(0, 0), p1)
        val i2 = moveIntent(r2, Position(2, 0), p1)
        val actions = rerouting.resolveCollisions(Seq(i1, i2))
        actions(r1) shouldBe Action.Move
        actions(r2) shouldBe Action.Wait

      "block both robots in direct collisions" in:
        val i1 = moveIntent(r1, p0, p1)
        val i2 = moveIntent(r2, p1, p0)
        val actions = rerouting.resolveCollisions(Seq(i1, i2))
        actions shouldBe Map(r1 -> Action.Wait, r2 -> Action.Wait)
