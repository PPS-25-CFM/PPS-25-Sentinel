package it.unibo.sentinel.core.collisions

import it.unibo.sentinel.UnitTest
import it.unibo.sentinel.core.mission.{Mission, MissionId, Priority}
import it.unibo.sentinel.core.robot.{Robot, RobotId}
import it.unibo.sentinel.core.routing.{Path, Step}
import it.unibo.sentinel.core.scenario.Placement
import it.unibo.sentinel.core.simulation.Tick
import it.unibo.sentinel.core.warehouse.Position
import it.unibo.sentinel.core.scenario.Intent

trait CollisionCheckerFixture:
  self: UnitTest =>

  val mission: Mission =
    Mission.relocate(MissionId("M"), Position(1, 1), Tick(10), Priority.normal)

  val r1: Robot = Robot.drone(RobotId("R1"))
  val r2: Robot = Robot.drone(RobotId("R2"))
  val r3: Robot = Robot.drone(RobotId("R3"))
  val r4: Robot = Robot.drone(RobotId("R4"))
  val r5: Robot = Robot.drone(RobotId("R5"))

  val group1: Seq[Robot] = Seq(r1, r2, r3)

  val p4 = Placement(r4, Position(4, 4))
  val p5 = Placement(r5, Position(5, 5))
  r4.follow(Path(Step(Position(5, 5), Tick.unit)))
  r5.follow(Path(Step(Position(4, 4), Tick.unit)))

  protected def toIntent(placement: Placement): Intent =
    Intent(placement.robot.id, placement.at, placement.next, Some(mission))

class CollisionCheckerSpec extends UnitTest with CollisionCheckerFixture:

  "A collision checker" when:

    "checking direct collisions" should:

      "return a list of robots that are colliding face to face" in:
        r4.tick()
        r5.tick()
        val intents = Seq(toIntent(p4), toIntent(p5))
        CollisionChecker.directCollisions(
          intents
        ) should contain theSameElementsAs Seq(
          DirectCollision(r4.id, r5.id)
        )

    "checking indirect collisions" should:

      "return a list of groups of robots that collide" in:
        val target = Position(1, 1)
        val intents =
          group1.map(r => Intent(r.id, Position(0, 0), target, Some(mission)))
        CollisionChecker.indirectCollisions(
          intents
        ) should contain theSameElementsAs Seq(
          IndirectCollision(target, group1.map(_.id))
        )

      "not signal any collision between two robots moving to different positions" in:
        val intentA =
          Intent(RobotId("RA"), Position(0, 0), Position(1, 0), Some(mission))
        val intentB =
          Intent(RobotId("RB"), Position(2, 0), Position(2, 0), Some(mission))
        val collisions =
          CollisionChecker.indirectCollisions(Seq(intentA, intentB))
        collisions should contain theSameElementsAs Seq(
          IndirectCollision(Position(1, 0), Seq(RobotId("RA"))),
          IndirectCollision(Position(2, 0), Seq(RobotId("RB")))
        )
