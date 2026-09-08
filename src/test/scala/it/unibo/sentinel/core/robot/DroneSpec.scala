package it.unibo.sentinel.core.robot

import it.unibo.sentinel.UnitTest
import it.unibo.sentinel.core.item.Item
import it.unibo.sentinel.core.mission.{Mission, MissionId}
import it.unibo.sentinel.core.simulation.Tick
import it.unibo.sentinel.core.warehouse.Position

class DroneSpec extends UnitTest with RobotFixture with RobotBehavior:

  private val deliverId = MissionId("D1")
  private val deliver: Mission =
    Mission.deliver(
      deliverId,
      Item.Computer,
      Position(9, 9),
      Position(8, 8),
      Tick(10)
    )

  "A Drone" when:
    behave like baseRobot(Robot.drone(robotId))

    "accepting missions" should:
      "accept a relocation mission" in:
        val robot = Robot.drone(robotId)
        robot.canAccept(mission1) shouldBe true
        robot.accept(mission1)
        robot.mission shouldBe Some(m1)

      "reject a delivery mission" in:
        val robot = Robot.drone(robotId)
        robot.canAccept(deliver) shouldBe false

      "keep no mission when offered a delivery" in:
        val robot = Robot.drone(robotId)
        robot.accept(deliver)
        robot.mission shouldBe None

    "managing items" should:
      "never pick or drop items" in:
        val robot = Robot.drone(robotId)
        robot.pick(Item.Computer) shouldBe false
        robot.drop(Item.Computer) shouldBe None
