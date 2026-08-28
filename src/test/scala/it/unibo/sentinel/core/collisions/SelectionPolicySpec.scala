package it.unibo.sentinel.core.collisions

import it.unibo.sentinel.UnitTest
import it.unibo.sentinel.core.robot.RobotId
import it.unibo.sentinel.core.robot.Robot

trait SelectionPolicyFixture:
  self: UnitTest =>

  val robots: Seq[Robot] = Seq(
    Robot(RobotId("R1")),
    Robot(RobotId("R2")),
    Robot(RobotId("R3")),
    Robot(RobotId("R4")),
    Robot(RobotId("R5"))
  )

class SelectionPolicySpec extends UnitTest with SelectionPolicyFixture:

  "A selection policy" when:

    "selecting randomly" should:
      val selections = 1
      val policy = SelectionPolicy.random(selections)

      "select random robots from a given list" in:
        val selection: Iterable[RobotId] = policy.select(robots)
        selection.size shouldBe selections
