package it.unibo.sentinel.core.robot

import it.unibo.sentinel.UnitTest
import it.unibo.sentinel.core.item.Item
import it.unibo.sentinel.core.mission.{Mission, MissionId}
import it.unibo.sentinel.core.simulation.Tick
import it.unibo.sentinel.core.warehouse.Position

trait CarrierBehavior extends RobotFixture:
  self: UnitTest =>

  private val deliverId = MissionId("D1")
  private val deliver: Mission =
    Mission.deliver(
      deliverId,
      Item.Computer,
      Position(9, 9),
      Position(8, 8),
      Tick(10)
    )

  def baseCarrier(build: => Robot): Unit =

    "accepting missions" should:

      "accept a relocation mission" in:
        val robot = build
        robot.canAccept(mission1) shouldBe true
        robot.accept(mission1)
        robot.mission shouldBe Some(m1)

      "accept a delivery mission when empty" in:
        val robot = build
        robot.canAccept(deliver) shouldBe true
        robot.accept(deliver)
        robot.mission shouldBe Some(deliverId)

      "reject a new mission when the queue is full" in:
        val robot = build
        robot.accept(mission1)
        robot.canAccept(mission2) shouldBe false
        robot.accept(mission2)
        robot.mission shouldBe Some(m1)

    "managing its bag" should:

      "pick an item that fits" in:
        val robot = build
        robot.pick(Item.Computer) shouldBe true

      "drop a carried item" in:
        val robot = build
        robot.pick(Item.Computer)
        robot.drop(Item.Computer) shouldBe Some(Item.Computer)

      "return None when dropping an item it does not carry" in:
        val robot = build
        robot.drop(Item.Computer) shouldBe None

      "drop only one copy when carrying duplicates" in:
        val robot = build
        robot.pick(Item.Computer) shouldBe true
        robot.pick(Item.Computer) shouldBe true
        robot.drop(Item.Computer) shouldBe Some(Item.Computer)
        robot.drop(Item.Computer) shouldBe Some(Item.Computer)
        robot.drop(Item.Computer) shouldBe None

    "clearing its route" should:

      "forget the path but keep the mission" in:
        val robot = build
        robot.accept(mission1)
        robot.follow(path)
        robot.status shouldBe RobotStatus.Moving
        robot.clearRoute()
        robot.path shouldBe None
        robot.mission shouldBe Some(m1)
        robot.status shouldBe RobotStatus.Ready
