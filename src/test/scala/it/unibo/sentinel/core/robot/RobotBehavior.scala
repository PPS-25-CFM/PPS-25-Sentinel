package it.unibo.sentinel.core.robot

import it.unibo.sentinel.UnitTest
import it.unibo.sentinel.core.mission.{Mission, MissionId}
import it.unibo.sentinel.core.warehouse.Position
import it.unibo.sentinel.core.routing.{Path, Step}
import it.unibo.sentinel.core.simulation.Tick

trait RobotFixture:
  val robotId: RobotId = RobotId("R1")
  val m1: MissionId = MissionId("M1")
  val m2: MissionId = MissionId("M2")
  val mission1: Mission = Mission.relocate(m1, Position(9, 9), Tick(10))
  val mission2: Mission = Mission.relocate(m2, Position(8, 8), Tick(10))

  val costs: Seq[Tick] = Seq(Tick(1), Tick(2), Tick(3))
  val positions: Seq[Position] =
    Seq(Position(1, 0), Position(2, 0), Position(3, 0))
  val steps: Seq[Step] =
    positions.zip(costs).map((pos, cost) => Step(pos, cost))
  val path: Path = Path(steps*)

trait RobotBehavior extends RobotFixture:
  selft: UnitTest =>

  def baseRobot(build: => Robot): Unit =

    "just created" should:
      val robot = build

      "have the given ID" in:
        robot.id shouldBe robotId

      "have no mission" in:
        robot.mission shouldBe None

      "be idle" in:
        robot.status shouldBe RobotStatus.Idle

      "have nothing to wait for" in:
        robot.remaining shouldBe Tick.zero

      "have zero workload" in:
        robot.workload shouldBe Workload.zero

    "without a mission" should:
      val robot = build

      "be able to accept one" in:
        robot.canAccept(mission1) shouldBe true

      "take the mission it is given" in:
        robot.accept(mission1)
        robot.mission shouldBe Some(m1)

      "track the accepted mission in its workload" in:
        val fresh = build
        fresh.accept(mission1)
        fresh.workload shouldBe Workload(1)

      "not be able to be paused" in:
        val previous = robot.status
        robot.pause()
        robot.status shouldBe previous

    "has a mission" should:

      "be ready to start it" in:
        val robot = build
        robot.accept(mission1)
        robot.status shouldBe RobotStatus.Ready

    "following a path" should:
      val robot = build
      robot.follow(path)

      "know the path it is following" in:
        robot.path.value.positions shouldBe positions

      "be moving" in:
        robot.status shouldBe RobotStatus.Moving

      "head to the first position of the path" in:
        robot.next shouldBe positions.headOption

      "wait the cost of the next position" in:
        val stepCost = costs.headOption.value
        robot.remaining shouldBe stepCost

      "be able to be paused" in:
        robot.pause()
        robot.status shouldBe RobotStatus.Waiting

    "stepping along a path" should:

      "move to the next position of the path if the remaining time is up" in:
        val robot = build
        robot.follow(path)
        robot.tick()
        robot.step()
        robot.next shouldBe positions.drop(1).headOption

      "not move to the next position of the path if the remaining time is not up" in:
        val robot = build
        robot.follow(path)
        robot.step()
        val currentNext = robot.next
        robot.step()
        robot.next shouldBe currentNext

    "releasing its mission" should:
      val robot = build
      robot.accept(mission1)
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
      val robot = build
      robot.follow(path)

      "decrease the remaining time to wait" in:
        val initialRemaining = robot.remaining
        robot.tick()
        robot.remaining shouldBe initialRemaining.previous
