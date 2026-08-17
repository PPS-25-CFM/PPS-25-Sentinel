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

      "not be able to accept a mission" in:
        simple.canAccept shouldBe false
