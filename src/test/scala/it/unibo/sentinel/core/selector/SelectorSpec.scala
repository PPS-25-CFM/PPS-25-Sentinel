package it.unibo.sentinel.core.selector

import it.unibo.sentinel.UnitTest
import it.unibo.sentinel.core.robot.Robot
import it.unibo.sentinel.core.warehouse.Position
import it.unibo.sentinel.core.selection.Selector
import it.unibo.sentinel.core.mission.*

class SelectorSpec extends UnitTest:

  val missionID = MissionId("M1")
  val duration: Ticks = 10
  val mission = Mission(
    missionID,
    Task.Move(Position(1, 1)),
    duration
  )
  val selector = Selector

  "A Selector" when:

    "asked to assign a mission" should:

      "return None if no candidate is present" in:
        val result = selector.choose(mission, Iterable.empty[(Robot, Position)])

        result shouldBe None
