package it.unibo.sentinel.core.collisions

import it.unibo.sentinel.UnitTest
import it.unibo.sentinel.core.item.Item
import it.unibo.sentinel.core.mission.{Mission, MissionId, Priority}
import it.unibo.sentinel.core.robot.{RobotId, value}
import it.unibo.sentinel.core.scenario.Intent
import it.unibo.sentinel.core.simulation.Tick
import it.unibo.sentinel.core.warehouse.Position

trait CollisionHandlerBehavior:
  this: UnitTest =>

  protected val r1: RobotId = RobotId("R1")
  protected val r2: RobotId = RobotId("R2")
  protected val r3: RobotId = RobotId("R3")
  protected val r4: RobotId = RobotId("R4")

  protected given SelectionPolicy = _.headOption.map(_.robotId)

  protected def createMission(id: String, target: Position): Mission =
    Mission.relocate(MissionId(s"m-$id"), target, Tick(10), Priority.normal)

  protected def createDeliverMission(
      id: String,
      from: Position,
      to: Position
  ): Mission =
    Mission.deliver(
      MissionId(s"m-deliver-$id"),
      Item.Computer,
      from,
      to,
      Tick(10),
      Priority.normal
    )

  protected def moveIntent(id: RobotId, from: Position, to: Position): Intent =
    Intent(id, from, to, Some(createMission(id.value, to)))

  protected def stationaryIntent(id: RobotId, pos: Position): Intent =
    Intent(id, pos, pos, None)

  def correctCollisionResolver(handler: CollisionHandler): Unit =

    "resolving indirect collisions" should:

      "allow the selected winner to move" in:
        val target = Position(1, 1)
        val i1 = moveIntent(r1, Position(0, 0), target)
        val i2 = moveIntent(r2, Position(0, 1), target)

        val actions = handler.resolveCollisions(Seq(i1, i2))
        actions(r1) shouldBe Action.Move
        actions(r2) shouldNot be(Action.Move)

      "prevent contenders from moving if a stationary robot occupies the target cell" in:
        val target = Position(1, 1)
        val i1 = moveIntent(r1, Position(0, 0), target)
        val i2 = moveIntent(r2, Position(0, 1), target)
        val i3Stat = stationaryIntent(r3, target)

        val actions = handler.resolveCollisions(Seq(i1, i2, i3Stat))
        actions(r1) shouldNot be(Action.Move)
        actions(r2) shouldNot be(Action.Move)

      "ignore stationary robots and allow moving robots to proceed to empty cells" in:
        val i1Stat = stationaryIntent(r1, Position(0, 0))
        val i2 = moveIntent(r2, Position(0, 1), Position(1, 1))

        val actions = handler.resolveCollisions(Seq(i1Stat, i2))
        actions.get(r1) shouldBe None
        actions(r2) shouldBe Action.Move

    "handling chain dependencies" should:

      "cascade non-move decisions when a robot cannot move into an occupied cell" in:
        val i1 = moveIntent(r1, Position(0, 0), Position(1, 0))
        val i2 = moveIntent(r2, Position(1, 0), Position(2, 0))
        val i3 = moveIntent(r3, Position(2, 0), Position(3, 0))
        val i4Stat = stationaryIntent(r4, Position(3, 0))

        val actions = handler.resolveCollisions(Seq(i1, i2, i3, i4Stat))
        Seq(r1, r2, r3).foreach(r => actions(r) shouldNot be(Action.Move))
