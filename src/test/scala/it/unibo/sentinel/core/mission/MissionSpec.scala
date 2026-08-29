package it.unibo.sentinel.core.mission

import it.unibo.sentinel.UnitTest
import it.unibo.sentinel.core.robot.RobotId
import it.unibo.sentinel.core.simulation.Tick
import it.unibo.sentinel.core.warehouse.Position

class MissionSpec extends UnitTest:

  val missionID: MissionId = MissionId("M1")
  val target: Position = Position(1, 1)
  val duration: Tick = Tick(10)
  val robotID: RobotId = RobotId("R1")

  def pendingMission: Mission = Mission.relocate(missionID, target, duration)
  def assignedMission: Mission = pendingMission.assignTo(robotID)
  def completedMission: Mission = assignedMission.complete
  def failedMission: Mission = pendingMission.fail

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

      "have the initial task" in:
        pendingMission.task shouldBe Task.move(target)

      "have the initial Duration" in:
        pendingMission.deadline shouldBe duration

      "expose the current Action and Target" in:
        pendingMission.currentAction shouldBe Some(Action.Move(target))
        pendingMission.currentTarget shouldBe Some(target)

    "managing carrier assignment" should:

      "set the carrier and status to Assigned" in:
        assignedMission.carrier shouldBe Some(robotID)
        assignedMission.status shouldBe MissionStatus.Assigned
        assignedMission.isPending shouldBe false
        assignedMission.isOver shouldBe false

      "preserve currentAction and currentTarget when assigned" in:
        assignedMission.currentAction shouldBe Some(Action.Move(target))
        assignedMission.currentTarget shouldBe Some(target)

      "prevent re-assignment if already assigned" in:
        assignedMission.assignTo(RobotId("R2")) shouldBe assignedMission

      "unset carrier and revert to Pending on unassign" in:
        val unassigned = assignedMission.unassign
        unassigned.carrier shouldBe None
        unassigned.status shouldBe MissionStatus.Pending
        unassigned.isPending shouldBe true

    "completing its execution" should:

      "not complete a Pending mission" in:
        pendingMission.complete shouldBe pendingMission

      "update status to Completed, be Over and hide Action/Target when Assigned" in:
        completedMission.status shouldBe MissionStatus.Completed
        completedMission.isOver shouldBe true
        completedMission.currentAction shouldBe None
        completedMission.currentTarget shouldBe None

      "clear carrier when completing an Assigned mission" in:
        assignedMission.complete.carrier shouldBe None
        assignedMission.complete.status shouldBe MissionStatus.Completed

      "ignore further transitions" in:
        completedMission.assignTo(robotID) shouldBe completedMission
        completedMission.unassign shouldBe completedMission
        completedMission.complete shouldBe completedMission
        completedMission.fail shouldBe completedMission

    "failing its execution" should:

      "update status to Failed, be Over and hide Action/Target" in:
        failedMission.status shouldBe MissionStatus.Failed
        failedMission.isOver shouldBe true
        failedMission.currentAction shouldBe None
        failedMission.currentTarget shouldBe None

      "clear carrier" in:
        assignedMission.fail.carrier shouldBe None
        assignedMission.fail.status shouldBe MissionStatus.Failed

      "ignore further transitions" in:
        failedMission.fail shouldBe failedMission
        failedMission.complete shouldBe failedMission
        failedMission.assignTo(robotID) shouldBe failedMission

    "managing actions" should:

      "advance, complete and clear carrier when Assigned" in:
        val done = assignedMission.completeCurrentAction
        done.task shouldBe Task.Done
        done.status shouldBe MissionStatus.Completed
        done.isOver shouldBe true
        done.carrier shouldBe None
        done.currentAction shouldBe None
        done.currentTarget shouldBe None

      "do nothing if Pending" in:
        pendingMission.completeCurrentAction shouldBe pendingMission

      "do nothing if Over" in:
        completedMission.completeCurrentAction shouldBe completedMission
        failedMission.completeCurrentAction shouldBe failedMission

    "proceeding through time" should:

      "decrease duration by 1 Tick" in:
        pendingMission.tick.deadline shouldBe duration.previous
        assignedMission.tick.deadline shouldBe duration.previous
        pendingMission.tick.task shouldBe pendingMission.task

      "expire and fail, clearing carrier if Assigned" in:
        val lastTickPending =
          Mission.relocate(MissionId("M_EXP"), target, Tick(1))
        val expiredPending = lastTickPending.tick
        expiredPending.deadline shouldBe Tick(0)
        expiredPending.status shouldBe MissionStatus.Failed
        expiredPending.isOver shouldBe true
        expiredPending.currentTarget shouldBe None

        val lastTickAssigned = Mission
          .relocate(MissionId("M_EXP2"), target, Tick(1))
          .assignTo(robotID)
        val expiredAssigned = lastTickAssigned.tick
        expiredAssigned.deadline shouldBe Tick(0)
        expiredAssigned.status shouldBe MissionStatus.Failed
        expiredAssigned.carrier shouldBe None

      "not decrease duration or change action if already Over" in:
        completedMission.tick shouldBe completedMission
        failedMission.tick shouldBe failedMission
