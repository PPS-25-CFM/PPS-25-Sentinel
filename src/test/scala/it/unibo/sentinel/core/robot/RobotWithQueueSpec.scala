package it.unibo.sentinel.core.robot

import it.unibo.sentinel.UnitTest
import it.unibo.sentinel.core.mission.{Mission, MissionId}
import it.unibo.sentinel.core.warehouse.Position
import it.unibo.sentinel.core.simulation.Tick

class RobotWithQueueSpec extends UnitTest with RobotFixture with RobotBehavior:

  val capacity: Int = 3

  "A Robot with queue" when:
    behave like baseRobot(Robot.drone(robotId, capacity))

    "does not have a full queue" should:

      "be able to accept multiple missions" in:
        val robot = Robot.drone(robotId, capacity)
        val mission = Mission.relocate(MissionId("M"), Position(9, 9), Tick(10))
        for _ <- 0 until capacity do
          robot.canAccept(mission) shouldBe true
          robot.accept(mission)
        robot.canAccept(mission) shouldBe false
