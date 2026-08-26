package it.unibo.sentinel.core.robot

import it.unibo.sentinel.UnitTest
import it.unibo.sentinel.core.mission.MissionId
import it.unibo.sentinel.core.warehouse.Position
import it.unibo.sentinel.core.routing.Path
import it.unibo.sentinel.core.simulation.Tick

trait RobotFixture:

  val robotId: RobotId = RobotId("R1")
  val m1: MissionId = MissionId("M1")
  val m2: MissionId = MissionId("M2")

  val stepCost: Tick = Tick(1)
  val steps: Seq[Position] = Seq(Position(1, 0), Position(2, 0), Position(3, 0))
  val path: Path = steps.map(_ -> stepCost)

class SimpleRobotSpec extends UnitTest with RobotFixture:

  "A SimpleRobot" when:

    "just created" should:
      val robot = Robot(robotId)

      "have the given ID" in:
        robot.id shouldBe robotId

      "have no mission" in:
        robot.mission shouldBe None

      "be idle" in:
        robot.status shouldBe RobotStatus.Idle

      "have nothing to wait for" in:
        robot.remaining shouldBe Tick(0)

    "without a mission" should:
      val robot = Robot(robotId)

      "be able to accept one" in:
        robot.canAccept shouldBe true

      "take the mission it is given" in:
        robot.accept(m1)
        robot.mission shouldBe Some(m1)

      "be ready to start it" in:
        robot.accept(m1)
        robot.status shouldBe RobotStatus.Ready

    "already on a mission" should:
      val robot = Robot(robotId)
      robot.accept(m1)

      "not be able to accept another one" in:
        robot.canAccept shouldBe false

      "keep its current mission when offered another" in:
        robot.accept(m2)
        robot.mission shouldBe Some(m1)

    "following a path" should:
      val robot = Robot(robotId)
      robot.follow(path)

      "know the path it is following" in:
        robot.path shouldBe Some(steps)

      "be moving" in:
        robot.status shouldBe RobotStatus.Moving

      "head to the first position of the path" in:
        robot.next shouldBe steps.headOption

      "wait the cost of the next position, minus the routing tick" in:
        robot.remaining shouldBe stepCost.previous

      "have nowhere left to go once the path is over" in:
        steps.foreach(_ => robot.step())
        robot.next shouldBe None

    "releasing its mission" should:
      val robot = Robot(robotId)
      robot.accept(m1)
      robot.follow(path)
      robot.release()

      "return to idle" in:
        robot.status shouldBe RobotStatus.Idle

      "forget the mission" in:
        robot.mission shouldBe None

      "forget the path" in:
        robot.path shouldBe None

      "have nothing to wait for" in:
        robot.remaining shouldBe Tick(0)
