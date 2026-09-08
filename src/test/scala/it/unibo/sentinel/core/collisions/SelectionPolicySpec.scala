package it.unibo.sentinel.core.collisions

import it.unibo.sentinel.UnitTest
import it.unibo.sentinel.core.robot.RobotId
import it.unibo.sentinel.core.robot.Robot
import it.unibo.sentinel.core.mission.Mission
import it.unibo.sentinel.core.mission.MissionId
import it.unibo.sentinel.core.warehouse.Position
import it.unibo.sentinel.core.simulation.Tick
import it.unibo.sentinel.core.mission.Priority

trait SelectionPolicyFixture:
  self: UnitTest =>

  val robots: Seq[Robot] = Seq(
    Robot.drone(RobotId("R1")),
    Robot.drone(RobotId("R2")),
    Robot.drone(RobotId("R3")),
    Robot.drone(RobotId("R4")),
    Robot.drone(RobotId("R5"))
  )
  val missions: Seq[Mission] = Seq(
    Mission.relocate(MissionId("M1"), Position(1, 1), Tick(1), Priority(1)),
    Mission.relocate(MissionId("M2"), Position(2, 2), Tick(2), Priority(2)),
    Mission.relocate(MissionId("M3"), Position(3, 3), Tick(3), Priority(3)),
    Mission.relocate(MissionId("M4"), Position(4, 4), Tick(4), Priority(4)),
    Mission.relocate(MissionId("M5"), Position(5, 5), Tick(5), Priority(5))
  )
  for
    i <- 0 until 5
    robot = robots(i)
    mission = missions(i)
  do robot.accept(mission)

class SelectionPolicySpec extends UnitTest with SelectionPolicyFixture:

  "A selection policy" when:
    given Seq[Mission] = missions

    "selecting randomly" should:
      val policy = SelectionPolicy.random()

      "select random robots from a given list" in:
        policy.select(robots) shouldBe defined

    "selecting based on mission deadline" should:
      val policy = SelectionPolicy.closestDeadline()

      "select the robot with the mission closest to failing" in:
        policy.select(robots) shouldBe Some(robots(0).id)

    "selecting based on mission priority" should:
      val policy = SelectionPolicy.highestPriority()

      "select the robot with the highest priority mission" in:
        policy.select(robots) shouldBe robots.lastOption.map(_.id)
