package it.unibo.sentinel.core.collisions

import it.unibo.sentinel.UnitTest
import it.unibo.sentinel.core.robot.{Robot, RobotId}
import it.unibo.sentinel.core.routing.{Path, Step}
import it.unibo.sentinel.core.scenario.Placement
import it.unibo.sentinel.core.simulation.Tick
import it.unibo.sentinel.core.warehouse.Position
import it.unibo.sentinel.core.scenario.Intent

trait CollisionCheckerFixture:
  self: UnitTest =>

  val r1: Robot = Robot.drone(RobotId("R1"))
  val r2: Robot = Robot.drone(RobotId("R2"))
  val r3: Robot = Robot.drone(RobotId("R3"))
  val r4: Robot = Robot.drone(RobotId("R4"))
  val r5: Robot = Robot.drone(RobotId("R5"))

  val p1 = Placement(r1, Position(0, 0))
  val p2 = Placement(r2, Position(1, 0))
  val p3 = Placement(r3, Position(3, 3))
  val p4 = Placement(r4, Position(5, 3))
  val p5 = Placement(r5, Position(4, 4))

  protected def toIntent(placement: Placement): Intent =
    Intent(placement.robot.id, placement.at, placement.next, None)

  protected def singleStep(target: Position): Path =
    Path(Step(target, Tick.zero))

class CollisionCheckerSpec extends UnitTest with CollisionCheckerFixture:

  "A collision checker" when:

    "checking direct collisions" should:

      "return a list of robots that are colliding face to face" in:
        r1.follow(singleStep(Position(1, 0)))
        r2.follow(singleStep(Position(0, 0)))
        val intents = Seq(toIntent(p1), toIntent(p2))
        val collisions = CollisionChecker.directCollisions(intents)
        collisions shouldBe Seq(DirectCollision(r1.id, r2.id))

    "checking indirect collisions" should:

      "return a list of groups of robots that (will) collide" in:
        val target = Position(4, 3)
        val group = Seq(p3, p4, p5)
        group.foreach(_.robot.follow(singleStep(target)))
        val intents = group.map(toIntent)
        val collisions = CollisionChecker.indirectCollisions(intents)
        val expected = Seq(IndirectCollision(target, group.map(_.robot.id)))
        collisions shouldBe expected

    "there are no collisions" should:

      "not signal any type of collision between two robots moving to different positions" in:
        r1.follow(singleStep(Position(0, 1)))
        r2.follow(singleStep(Position(1, 1)))
        val notColliding = Seq(toIntent(p1), toIntent(p2))
        val indirect = CollisionChecker.indirectCollisions(notColliding)
        val direct = CollisionChecker.directCollisions(notColliding)
        indirect shouldBe Seq(
          IndirectCollision(p1.next, Seq(p1.robot.id)),
          IndirectCollision(p2.next, Seq(p2.robot.id))
        )
        direct shouldBe empty
