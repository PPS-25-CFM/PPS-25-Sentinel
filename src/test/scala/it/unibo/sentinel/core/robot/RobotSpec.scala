package it.unibo.sentinel.core.robot

import it.unibo.sentinel.UnitTest

class RobotSpec extends UnitTest:

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
        val missionId: MissionId = "M1"
        simple.accept(missionId)
        simple.mission shouldBe Some(missionId)

    "has a mission" should:
      val withMission: Robot = Robot(id)
      withMission.accept("M1")

      "not be able to accept a mission" in:
        withMission.canAccept shouldBe false

      "not accept a mission" in:
        val missionId: MissionId = "M2"
        withMission.accept(missionId)
        withMission.mission should not be Some(missionId)

      "be ready to start the mission" in:
        withMission.status shouldBe RobotStatus.Ready

      "start the mission" in:
        withMission.startMission
        withMission.status shouldBe RobotStatus.Moving

    "dropping the mission" should:
      val dropped: Robot = Robot(id)
      dropped.accept("M1")
      dropped.dropMission

      "return to idle" in:
        dropped.status shouldBe RobotStatus.Idle
