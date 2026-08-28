package it.unibo.sentinel.core.robot

import it.unibo.sentinel.UnitTest
import it.unibo.sentinel.core.mission.MissionId
import it.unibo.sentinel.core.warehouse.Position
import it.unibo.sentinel.core.routing.Path
import it.unibo.sentinel.core.simulation.Tick
import it.unibo.sentinel.core.routing.Step

trait RobotFixture:

  val robotId: RobotId = RobotId("R1")
  val m1: MissionId = MissionId("M1")
  val m2: MissionId = MissionId("M2")

  val costs: Seq[Tick] = Seq(Tick(1), Tick(2), Tick(3))
  val steps: Seq[Position] = Seq(Position(1, 0), Position(2, 0), Position(3, 0))
  val legs = steps.zip(costs).map((pos, cost) => Step(pos, cost))
  val path: Path = Path(legs*)

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
        robot.remaining shouldBe Tick.zero

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
        robot.path.value.positions shouldBe steps

      "be moving" in:
        robot.status shouldBe RobotStatus.Moving

      "head to the first position of the path" in:
        robot.next shouldBe steps.headOption

      "wait the cost of the next position" in:
        val stepCost = costs.headOption.value
        robot.remaining shouldBe stepCost

    "stepping along a path" should:

      "move to the next position of the path if the remaining time is up" in:
        val robot = Robot(robotId)
        robot.follow(path)
        robot.tick()
        robot.step()
        robot.next shouldBe steps.drop(1).headOption

      "not move to the next position of the path if the remaining time is not up" in:
        val robot = Robot(robotId)
        robot.follow(path)
        robot.step()
        val currentNext = robot.next
        robot.step()
        robot.next shouldBe currentNext

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
        robot.remaining shouldBe Tick.zero

    "ticked" should:
      val robot = Robot(robotId)
      robot.follow(path)

      "decrease the remaining time to wait" in:
        val initialRemaining = robot.remaining
        robot.tick()
        robot.remaining shouldBe initialRemaining.previous
