package it.unibo.sentinel.core.robot

import it.unibo.sentinel.UnitTest
import it.unibo.sentinel.core.item.Item

class CarrierSpec
    extends UnitTest
    with RobotFixture
    with RobotBehavior
    with CarrierBehavior
    with PaceBehavior
    with QueuedBehavior:

  "A LightCarrier" when:
    behave like baseRobot(Robot.lightCarrier(robotId), Speed.normal)
    behave like pacedRobot(Robot.lightCarrier(robotId), Speed.normal)
    behave like queuedRobot(Robot.lightCarrier(robotId, _), Capacity.large)
    behave like baseCarrier(Robot.lightCarrier(robotId))

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
    behave like baseRobot(Robot.heavyCarrier(robotId), Speed.slow)
    behave like pacedRobot(Robot.heavyCarrier(robotId), Speed.slow)
    behave like queuedRobot(Robot.heavyCarrier(robotId, _), Capacity.small)
    behave like baseCarrier(Robot.heavyCarrier(robotId))

    "managing its load limit" should:
      "carry more than a light carrier" in:
        val robot = Robot.heavyCarrier(robotId)
        robot.pick(Item.Fridge) shouldBe true // load 50/200
        robot.pick(Item.Computer) shouldBe true // load 51/200
