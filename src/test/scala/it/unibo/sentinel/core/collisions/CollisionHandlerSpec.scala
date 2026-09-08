package it.unibo.sentinel.core.collisions

import it.unibo.sentinel.UnitTest
import it.unibo.sentinel.core.mission.MissionId
import it.unibo.sentinel.core.robot.Robot
import it.unibo.sentinel.core.scenario.Placement
import it.unibo.sentinel.core.warehouse.Position
import it.unibo.sentinel.core.mission.Mission
import it.unibo.sentinel.core.mission.Priority
import it.unibo.sentinel.core.simulation.Tick

trait CollisionHandlerFixture extends CollisionCheckerFixture:
  self: UnitTest =>

  given policy: SelectionPolicy = SelectionPolicy.random()
  val pausing: CollisionHandler = CollisionHandler.pausing()
  (group1 ++ group2).zipWithIndex.foreach { (robot, idx) =>
    robot.accept(
      Mission.relocate(
        MissionId(s"m-$idx"),
        Position(1, 1),
        Tick(10),
        Priority.normal
      )
    )
  }
  (group1 ++ group2).foreach(_.tick())

  val p1: Placement = Placement(r1, Position(0, 0))
  val p2: Placement = Placement(r2, Position(0, 1))
  val p3: Placement = Placement(r3, Position(1, 1))

class CollisionHandlerSpec extends UnitTest with CollisionHandlerFixture:

  "A PauseCollisionHandler" when:

    "resolving indirect collisions" should:

      "unblock the winner and block all moving losers when the target is free" in:
        r1.pause()
        val actions = pausing.resolveIndirectCollisions(p1, Seq(p2))
        actions should contain theSameElementsAs Seq(
          Action.Unblock(r1.id),
          Action.Block(r2.id)
        )

      "block the winner and moving losers if a stationary loser occupies the target position" in:
        r1.resume()
        r2.resume()
        val r3Stationary = Robot.drone(r3.id)
        val p3Stationary = Placement(r3Stationary, Position(1, 1))
        val actions =
          pausing.resolveIndirectCollisions(p1, Seq(p2, p3Stationary))
        actions should contain theSameElementsAs Seq(
          Action.Block(r1.id),
          Action.Block(r2.id)
        )

      "only block moving losers when the winner is stationary" in:
        val r1Stationary = Robot.drone(r1.id)
        r1Stationary.accept(mission)
        val p1Stationary = Placement(r1Stationary, Position(0, 0))
        val actions = pausing.resolveIndirectCollisions(p1Stationary, Seq(p2))
        actions shouldBe Seq(Action.Block(r2.id))

    "resolving direct collisions" should:

      "block both robots when they are moving against each other" in:
        r4.resume()
        r5.resume()
        val actions = pausing.resolveDirectCollisions(p4, p5)
        actions shouldBe Seq(Action.Block(r4.id), Action.Block(r5.id))

    "performing cleanup" should:

      "emit Block action only for ready moving robots that cannot move" in:
        r4.resume()
        r5.resume()
        val actions = pausing.cleanup(Seq(p4, p5))
        actions should contain theSameElementsAs Seq(
          Action.Block(r4.id),
          Action.Block(r5.id)
        )
