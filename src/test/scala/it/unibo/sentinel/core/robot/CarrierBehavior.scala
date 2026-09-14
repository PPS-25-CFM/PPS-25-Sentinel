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

  private def saturateToUncarriable(robot: Robot): Unit =
    while robot.pick(Item.Computer) do ()

  def baseCarrier(build: => Robot): Unit =

    "accepting missions" should:

      "accept a relocation mission" in:
        val robot = build
        robot.canAccept(mission1) shouldBe true
        robot.accept(mission1)
        robot.mission shouldBe Some(m1)

      "accept a delivery mission if it can carry the item" in:
        val robot = build
        robot.canAccept(deliver) shouldBe true
        robot.accept(deliver)
        robot.mission shouldBe Some(deliverId)

      "reject a delivery mission if it cannot carry the item" in:
        val robot = build
        saturateToUncarriable(robot)
        robot.canAccept(deliver) shouldBe false
        robot.accept(deliver)
        robot.mission shouldBe None

    "managing its bag" should:

      "pick an item that fits" in:
        val robot = build
        robot.pick(Item.Computer) shouldBe true

      "reject an item that would exceed maxLoad" in:
        val robot = build
        saturateToUncarriable(robot)
        robot.pick(Item.Computer) shouldBe false

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
