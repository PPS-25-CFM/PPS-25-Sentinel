package it.unibo.sentinel.core.mission

import it.unibo.sentinel.UnitTest
import it.unibo.sentinel.core.mission.*

class MissionSpec extends UnitTest:

  val missionID = MissionID("M1")
  val target = (1, 1)
  val mission = Mission(missionID, target)

  "A Mission" when:

    "newly created" should:

      "have the right ID" in:
        mission.id shouldBe missionID

      "have no Robot assigned to" in:
        mission.carrier shouldBe None
      
      "be Pending" in:
        mission.status shouldBe MissionStatus.Pending

      "have a Destination" in:
        mission.destination shouldBe target

    "managing the assignment to a Robot" should:
      val robotID: RobotID = "R1"
      val replacer: RobotID = "R2"

      "set the carrier" in:
        mission.assignTo(robotID).carrier shouldBe Some(robotID)

      "update the status to Assigned" in:
        mission.assignTo(robotID).status shouldBe MissionStatus.Assigned

      "be possible only if pending" in:
        val assigned = mission.assignTo(robotID)

        assigned.assignTo(replacer) shouldBe assigned
        
