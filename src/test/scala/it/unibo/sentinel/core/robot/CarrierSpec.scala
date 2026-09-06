package it.unibo.sentinel.core.robot

import it.unibo.sentinel.UnitTest
import it.unibo.sentinel.core.item.{Item, ItemWeight}
import it.unibo.sentinel.core.mission.{Mission, MissionId}
import it.unibo.sentinel.core.simulation.Tick
import it.unibo.sentinel.core.warehouse.Position
import it.unibo.sentinel.core.robot.RobotStatus

class CarrierSpec extends UnitTest with RobotFixture with RobotBehavior:

  private val maxLoad = ItemWeight(10)
  private val deliverId = MissionId("D1")
  private val deliver: Mission =
    Mission.deliver(
      deliverId,
      Item.Computer,
      Position(9, 9),
      Position(8, 8),
      Tick(10)
    )

  "A Carrier" when:
    behave like baseRobot(Robot.carrier(robotId, maxLoad))

    "accepting missions" should:
      "accept a relocation mission" in:
        val robot = Robot.carrier(robotId, maxLoad)
        robot.canAccept(mission1) shouldBe true
        robot.accept(mission1)
        robot.mission shouldBe Some(m1)

      "accept a delivery mission when empty" in:
        val robot = Robot.carrier(robotId, maxLoad)
        robot.canAccept(deliver) shouldBe true
        robot.accept(deliver)
        robot.mission shouldBe Some(deliverId)

      "reject a new mission when the queue is full" in:
        val robot = Robot.carrier(robotId, maxLoad, capacity = 1)
        robot.accept(mission1)
        robot.canAccept(mission2) shouldBe false
        robot.accept(mission2)
        robot.mission shouldBe Some(m1)

      "accept up to capacity missions" in:
        val capacity = 3
        val robot = Robot.carrier(robotId, maxLoad, capacity)
        for i <- 0 until capacity do
          val m = Mission.relocate(MissionId(s"M$i"), Position(9, 9), Tick(10))
          robot.canAccept(m) shouldBe true
          robot.accept(m)
        robot.canAccept(mission2) shouldBe false

    "managing its bag" should:
      "pick an item that fits" in:
        val robot = Robot.carrier(robotId, maxLoad)
        robot.pick(Item.Computer) shouldBe true

      "reject an item that would exceed maxLoad" in:
        val robot = Robot.carrier(robotId, maxLoad)
        robot.pick(Item.Computer) shouldBe true // load 1/10
        robot.pick(Item.Table) shouldBe false // 1 + 10 > 10

      "accept an item that exactly fills maxLoad" in:
        val robot = Robot.carrier(robotId, maxLoad)
        robot.pick(Item.Table) shouldBe true // 10/10

      "reject a single item heavier than maxLoad" in:
        val robot = Robot.carrier(robotId, ItemWeight(1))
        robot.pick(Item.Fridge) shouldBe false // 50 > 1

      "drop a carried item" in:
        val robot = Robot.carrier(robotId, maxLoad)
        robot.pick(Item.Computer)
        robot.drop(Item.Computer) shouldBe Some(Item.Computer)

      "return None when dropping an item it does not carry" in:
        val robot = Robot.carrier(robotId, maxLoad)
        robot.drop(Item.Computer) shouldBe None

      "free space after a drop" in:
        val robot = Robot.carrier(robotId, maxLoad)
        robot.pick(Item.Computer) shouldBe true
        robot.pick(Item.Table) shouldBe false
        robot.drop(Item.Computer) shouldBe Some(Item.Computer)
        robot.pick(Item.Table) shouldBe true

      "drop only one copy when carrying duplicates" in:
        val robot = Robot.carrier(robotId, maxLoad)
        robot.pick(Item.Computer) shouldBe true
        robot.pick(Item.Computer) shouldBe true
        robot.drop(Item.Computer) shouldBe Some(Item.Computer)
        robot.drop(Item.Computer) shouldBe Some(Item.Computer)
        robot.drop(Item.Computer) shouldBe None

    "clearing its route" should:
      "forget the path but keep the mission" in:
        val robot = Robot.carrier(robotId, maxLoad)
        robot.accept(mission1)
        robot.follow(path)
        robot.status shouldBe RobotStatus.Moving
        robot.clearRoute()
        robot.path shouldBe None
        robot.mission shouldBe Some(m1)
        robot.status shouldBe RobotStatus.Ready
