package it.unibo.sentinel.core.robot

import it.unibo.sentinel.UnitTest

class RobotSpec extends UnitTest:
  
  "A simple robot" when:
    val id: RobotId = RobotId("R1")
    val simple: Robot = Robot(id)

    "created" should:

      "have an ID" in:
        simple.id shouldBe id