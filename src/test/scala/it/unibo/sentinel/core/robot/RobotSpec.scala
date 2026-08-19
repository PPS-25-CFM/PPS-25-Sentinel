package it.unibo.sentinel.core.robot

import it.unibo.sentinel.UnitTest
import it.unibo.sentinel.core.mission.MissionId
import it.unibo.sentinel.core.warehouse.Position
import it.unibo.sentinel.core.routing.Path

class RobotSpec extends UnitTest:

  val m1 = MissionId("M1")
  val m2 = MissionId("M2")

  "A simple robot" when:
    val id: RobotId = RobotId("R1")
    val simple: Robot = Robot(id)

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
        simple.accept(m1)
        simple.mission shouldBe Some(m1)

    "has a mission" should:
      val withMission: Robot = Robot(id)
      withMission.accept(m1)

      "not be able to accept a mission" in:
        withMission.canAccept shouldBe false

      "not accept a mission" in:
        withMission.accept(m2)
        withMission.mission should not be Some(m2)

      "be ready to start the mission" in:
        withMission.status shouldBe RobotStatus.Ready

      "follow a path" in:
        val path: Path = Seq(Position(1, 0), Position(2, 0), Position(3, 0))
        withMission.follow(path)
        withMission.path shouldBe Some(path)
        withMission.status shouldBe RobotStatus.Moving
        withMission.next shouldBe Some(Position(1, 0))

    "dropping the mission" should:
      val dropped: Robot = Robot(id)
      dropped.accept(m1)
      dropped.release()

      "return to idle" in:
        dropped.status shouldBe RobotStatus.Idle

      "remove the mission" in:
        dropped.mission shouldBe None
