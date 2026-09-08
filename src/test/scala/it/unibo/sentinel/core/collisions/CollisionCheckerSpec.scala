package it.unibo.sentinel.core.collisions

import it.unibo.sentinel.UnitTest
import it.unibo.sentinel.core.robot.Robot
import it.unibo.sentinel.core.robot.RobotId
import it.unibo.sentinel.core.warehouse.Position
import it.unibo.sentinel.core.routing.Path
import it.unibo.sentinel.core.scenario.Placement
import it.unibo.sentinel.core.routing.Step
import it.unibo.sentinel.core.simulation.Tick
import it.unibo.sentinel.core.mission.MissionId
import it.unibo.sentinel.core.mission.Mission
import it.unibo.sentinel.core.mission.Priority

trait CollisionCheckerFixture:
  self: UnitTest =>

  val mission: Mission =
    Mission.relocate(MissionId("M"), Position(1, 1), Tick(10), Priority.normal)
  val path1: Path = Path(Step(Position(1, 1), Tick.unit))
  val path2: Path = Path(Step(Position(5, 5), Tick.unit))

  val r1: Robot = Robot.drone(RobotId("R1"))
  val r2: Robot = Robot.drone(RobotId("R2"))
  val r3: Robot = Robot.drone(RobotId("R3"))
  val r4: Robot = Robot.drone(RobotId("R4"))
  val r5: Robot = Robot.drone(RobotId("R5"))

  val group1: Seq[Robot] = Seq(r1, r2, r3)
  val group2: Seq[Robot] = Seq(r4, r5)

  group1.foreach(_.follow(path1))
  val p4 = Placement(r4, Position(4, 4))
  val p5 = Placement(r5, Position(5, 5))
  r4.follow(Path(Step(Position(5, 5), Tick.unit)))
  r5.follow(Path(Step(Position(4, 4), Tick.unit)))

class CollisionCheckerSpec extends UnitTest with CollisionCheckerFixture:

  "A collision checker" when:

    "checking if a robot can move" should:

      "return false if not enough ticks passed" in:
        CollisionChecker.canMove(p4, Seq()) shouldBe false

      "return true if the ticks needed are zero" in:
        p4.robot.tick()
        CollisionChecker.canMove(p4, Seq()) shouldBe true

      "return false if the robot is either Idle or Ready" in:
        val idle: Robot = Robot.drone(RobotId("idle"))
        val ready: Robot = Robot.drone(RobotId("Ready"))
        ready.accept(mission)
        idle.tick()
        ready.tick()
        CollisionChecker.canMove(
          Placement(idle, Position(0, 0)),
          Seq()
        ) shouldBe false
        CollisionChecker.canMove(
          Placement(ready, Position(0, 0)),
          Seq()
        ) shouldBe false

      "return true if the robot is blocked but there is no blockage" in:
        val blocked: Robot = Robot.drone(RobotId("Blocked"))
        blocked.accept(mission)
        blocked.follow(path1)
        blocked.tick()
        blocked.pause()
        CollisionChecker.canMove(
          Placement(blocked, Position(0, 0)),
          Seq()
        ) shouldBe true

      "return false if the robot is trying to move into another robot that can't move" in:
        val rA: Robot = Robot.drone(RobotId("A"))
        rA.accept(mission)
        rA.follow(path1)
        val rB: Robot = Robot.drone(RobotId("B"))
        CollisionChecker.canMove(
          Placement(rA, Position(0, 0)),
          Seq(Placement(rB, Position(1, 1)))
        ) shouldBe false

    "checking direct collisions" should:

      "return a list of robots that are colliding face to face" in:
        group2.foreach(_.tick())
        val intents = Seq(p4.intent, p5.intent)
        CollisionChecker.directCollisions(
          intents
        ) should contain theSameElementsAs Seq(
          DirectCollision(r4.id, r5.id)
        )

    "checking indirect collisions" should:

      "return a list of groups of robots that collide" in:
        group1.foreach(_.tick())
        val intents = group1.map(r => Placement(r, Position(0, 0)).intent)
        CollisionChecker.indirectCollisions(
          intents
        ) should contain theSameElementsAs Seq(
          IndirectCollision(Position(1, 1), group1.map(_.id))
        )

      "not signal any collision between two robots that want to move to the same position with different ticks" in:
        val rA = Robot.drone(RobotId("RA"))
        val rB = Robot.drone(RobotId("RB"))
        val pathA = Path(Step(Position(1, 0), Tick(1)))
        val pathB = Path(Step(Position(1, 0), Tick(2)))
        rA.follow(pathA)
        rB.follow(pathB)
        rA.tick()
        rB.tick()

        val intentA = Placement(rA, Position(0, 0)).intent
        val intentB = Placement(rB, Position(2, 0)).intent
        val collisions =
          CollisionChecker.indirectCollisions(Seq(intentA, intentB))
        collisions should contain theSameElementsAs Seq(
          IndirectCollision(Position(1, 0), Seq(rA.id)),
          IndirectCollision(Position(2, 0), Seq(rB.id))
        )
