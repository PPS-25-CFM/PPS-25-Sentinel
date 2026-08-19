package it.unibo.sentinel.core.mission

import it.unibo.sentinel.UnitTest
import it.unibo.sentinel.core.robot.RobotId
import it.unibo.sentinel.core.warehouse.Position

class MissionSpec extends UnitTest:

  val missionID: MissionId = MissionId("M1")
  val target: Position = Position(1, 1)
  val task: Task = Task.goto(target)
  val duration: Ticks = 10

  val pendingMission: Mission = Mission(missionID, task, duration)

  "A Mission" when:

    "newly created" should:

      "have the correct ID" in:
        pendingMission.id shouldBe missionID

      "have no Robot assigned" in:
        pendingMission.carrier shouldBe None

      "be in Pending status" in:
        pendingMission.status shouldBe MissionStatus.Pending
        pendingMission.isPending shouldBe true

      "not be considered Over" in:
        pendingMission.isOver shouldBe false

      "have the assigned Task" in:
        pendingMission.task shouldBe task

      "have the initial Duration" in:
        pendingMission.duration shouldBe duration

      "expose the current destination of its active Step" in:
        pendingMission.currentDestination shouldBe Some(target)

    "managing carrier assignment" should:
      val robotID = RobotId("R1")
      val replacerID = RobotId("R2")
      val assignedMission = pendingMission.assignTo(robotID)

      "set the carrier correctly" in:
        assignedMission.carrier shouldBe Some(robotID)

      "update the status to Assigned" in:
        assignedMission.status shouldBe MissionStatus.Assigned

      "preserve the current destination when assigned" in:
        assignedMission.currentDestination shouldBe Some(target)

      "prevent re-assignment to another robot if already assigned" in:
        assignedMission.assignTo(replacerID) shouldBe assignedMission

      "unset the carrier and revert status to Pending on unassign" in:
        val unassigned = assignedMission.unassign
        unassigned.carrier shouldBe None
        unassigned.status shouldBe MissionStatus.Pending

    "completing its execution" should:
      val completedMission = pendingMission.complete

      "update the status to Completed" in:
        completedMission.status shouldBe MissionStatus.Completed

      "be considered Over" in:
        completedMission.isOver shouldBe true

      "return None as currentDestination once Over" in:
        completedMission.currentDestination shouldBe None

      "ignore further assignment attempts" in:
        completedMission.assignTo(RobotId("R1")) shouldBe completedMission

      "ignore unassign attempts" in:
        completedMission.unassign shouldBe completedMission

    "failing its execution" should:
      val failedMission = pendingMission.fail

      "update the status to Failed" in:
        failedMission.status shouldBe MissionStatus.Failed

      "be considered Over" in:
        failedMission.isOver shouldBe true

      "return None as currentDestination once Over" in:
        failedMission.currentDestination shouldBe None

    "proceeding through time (Ticks)" should:

      "decrease the duration by 1 Tick when active" in:
        val nextMission = pendingMission.proceed
        nextMission.duration shouldBe (duration - 1)

      "expire and fail when duration reaches 0" in:
        val lastTickMission = Mission(MissionId("M_EXP"), task, duration = 1)
        val expiredMission = lastTickMission.proceed

        expiredMission.duration shouldBe 0
        expiredMission.status shouldBe MissionStatus.Failed
        expiredMission.isOver shouldBe true

      "not decrease duration or change status if already Over" in:
        val completedMission = pendingMission.complete
        val proceedAfterOver = completedMission.proceed

        proceedAfterOver shouldBe completedMission
