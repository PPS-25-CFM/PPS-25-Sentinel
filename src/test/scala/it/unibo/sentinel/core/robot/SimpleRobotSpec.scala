package it.unibo.sentinel.core.robot

import it.unibo.sentinel.UnitTest

class SimpleRobotSpec extends UnitTest with RobotFixture with RobotBehavior:

  "A SimpleRobot" when:
    behave like baseRobot(Robot.drone(robotId))

    "already on a mission" should:
      val robot = Robot.drone(robotId)
      robot.accept(mission1)

      "not be able to accept another one" in:
        robot.canAccept(mission2) shouldBe false

      "keep its current mission when offered another" in:
        robot.accept(mission2)
        robot.mission shouldBe Some(m1)
