package it.unibo.sentinel.core.robot

import it.unibo.sentinel.UnitTest
import it.unibo.sentinel.core.item.Item
import it.unibo.sentinel.core.mission.{Mission, MissionId}
import it.unibo.sentinel.core.simulation.Tick
import it.unibo.sentinel.core.warehouse.Position

class CarrierSpec
    extends UnitTest
    with RobotFixture
    with RobotBehavior
    with CarrierBehavior:

  "A LightCarrier" when:
    behave like baseRobot(Robot.lightCarrier(robotId))
    behave like baseCarrier(Robot.lightCarrier(robotId))

    "accepting missions" should:
      "accept up to capacity missions" in:
        val capacity = 3
        val robot = Robot.lightCarrier(robotId, capacity)
        for i <- 0 until capacity do
          val m = Mission.relocate(MissionId(s"M$i"), Position(9, 9), Tick(10))
          robot.canAccept(m) shouldBe true
          robot.accept(m)
        robot.canAccept(mission2) shouldBe false

    "managing its load limit" should:
      "pick an item that exactly fills maxLoad" in:
        val robot = Robot.lightCarrier(robotId)
        robot.pick(Item.Fridge) shouldBe true // 50/50

      "reject an item that would exceed maxLoad" in:
        val robot = Robot.lightCarrier(robotId)
        robot.pick(Item.Fridge) shouldBe true // load 50/50
        robot.pick(Item.Computer) shouldBe false // 50 + 1 > 50

      "free space after a drop" in:
        val robot = Robot.lightCarrier(robotId)
        robot.pick(Item.Fridge) shouldBe true
        robot.pick(Item.Computer) shouldBe false
        robot.drop(Item.Fridge) shouldBe Some(Item.Fridge)
        robot.pick(Item.Computer) shouldBe true

  "A HeavyCarrier" when:
    behave like baseRobot(Robot.heavyCarrier(robotId))
    behave like baseCarrier(Robot.heavyCarrier(robotId))

    "managing its load limit" should:
      "carry more than a light carrier" in:
        val robot = Robot.heavyCarrier(robotId)
        robot.pick(Item.Fridge) shouldBe true // load 50/200
        robot.pick(Item.Computer) shouldBe true // load 51/200
