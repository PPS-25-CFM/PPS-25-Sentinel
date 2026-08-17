package it.unibo.sentinel.core.mission

import it.unibo.sentinel.UnitTest
import it.unibo.sentinel.core.mission.*

class MissionSpec extends UnitTest:

  val missionID = MissionID("M1")
  val target: Position = (1, 1)
  val task: Task = Task.MoveTo(target)
  val duration: Ticks = 10
  val mission = Mission(missionID, task, duration)

  "A Mission" when:

    "newly created" should:

      "have the right ID" in:
        mission.id shouldBe missionID

      "have no Robot assigned to" in:
        mission.carrier shouldBe None
      
      "be Pending" in:
        mission.status shouldBe MissionStatus.Pending

      "have a Task" in:
        mission.task shouldBe task

      "have a Duration" in:
        mission.duration shouldBe duration

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

    "managing the unassignment off a Robot" should:
      val robotID: RobotID = "R1"
      val assigned = mission.assignTo(robotID)

      "unset the carrier" in:
        assigned.unassign.carrier shouldBe None

      "update the status to Pending" in:
        assigned.unassign.status shouldBe MissionStatus.Pending

    "completing its Task" should:
      val completed = mission.complete

      "be Complete" in:
        completed.status shouldBe MissionStatus.Completed

      "considered Over" in:
        completed.isOver shouldBe true

    "failing its Task" should:
      val completed = mission.fail

      "be Failed" in:
        completed.status shouldBe MissionStatus.Failed

      "considered Over" in:
        completed.isOver shouldBe true

    "proceeding through time" should:
      val next = mission.proceed
      
      "decrease the Duration" in:
        next.duration shouldBe duration - 1

      "expire if it reaches 0" in:
        val expired = Mission(MissionID("Expired"), task, 1).proceed

        expired.status shouldBe MissionStatus.Failed

