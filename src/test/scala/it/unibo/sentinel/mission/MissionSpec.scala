package it.unibo.sentinel.core.mission

import it.unibo.sentinel.UnitTest
import it.unibo.sentinel.core.mission.*

class MissionSpec extends UnitTest:

  val missionID = MissionID("M1")
  val mission = Mission(missionID)

  "A Mission" when:

    "newly created" should:

      "have the right ID" in:
        mission.id shouldBe missionID
