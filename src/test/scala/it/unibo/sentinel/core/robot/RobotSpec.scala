package it.unibo.sentinel.core.robot

import it.unibo.sentinel.UnitTest
import it.unibo.sentinel.core.mission.MissionId
import it.unibo.sentinel.core.warehouse.Position
import it.unibo.sentinel.core.routing.Path

trait RobotFixture:
  self: UnitTest =>
  val id: RobotId = RobotId("R1")
  val m1: MissionId = MissionId("M1")
  val m2: MissionId = MissionId("M2")
  val path: Path = Seq(Position(1, 0), Position(2, 0), Position(3, 0))

  def simple: Robot = Robot(id)

  def withMission(id: MissionId): Robot =
    val robot = simple
    robot.accept(id)
    robot

  def withPath(path: Path = path): Robot =
    val robot = simple
    robot.follow(path)
    robot

class RobotSpec extends UnitTest with RobotFixture:

  "A simple robot" when:

    "created" should:

      "have an ID" in:
        simple.id shouldBe id

      "not have a mission" in:
        simple.mission shouldBe None

      "be idle" in:
        simple.status shouldBe RobotStatus.Idle

    "does not have a mission" should:

      "be able to accept a mission" in:
        simple.canAccept shouldBe true

      "accept a mission" in:
        withMission(m1).mission shouldBe Some(m1)

    "has a mission" should:

      "not be able to accept a mission" in:
        withMission(m1).canAccept shouldBe false

      "not accept a mission" in:
        withMission(m1).accept(m2)
        withMission(m1).mission should not be Some(m2)

      "be ready to start the mission" in:
        withMission(m1).status shouldBe RobotStatus.Ready

    "following a path" should:

      "have the correct path and status" in:
        withPath().path shouldBe Some(path)
        withPath().status shouldBe RobotStatus.Moving
        withPath().next shouldBe Some(Position(1, 0))

      "step to the next position" in:
        val robot = withPath()
        robot.step()
        robot.next shouldBe Some(Position(2, 0))
        robot.step()
        robot.next shouldBe Some(Position(3, 0))
        robot.step()
        robot.next shouldBe None
        robot.step()
        robot.next shouldBe None

    "releasing the mission" should:
      val released: Robot = withMission(m1)
      released.release()

      "return to idle" in:
        released.status shouldBe RobotStatus.Idle

      "remove the mission" in:
        released.mission shouldBe None
