package it.unibo.sentinel.core.selector

import it.unibo.sentinel.UnitTest
import it.unibo.sentinel.core.robot.Robot
import it.unibo.sentinel.core.warehouse.Position
import it.unibo.sentinel.core.selection.Selector
import it.unibo.sentinel.core.mission.*
import org.mockito.Mockito
import org.mockito.Mockito.when

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
        val result = selector.choose(
          mission,
          Iterable.empty[(Robot, Position)]
        )

        result shouldBe None

    "return None if no candidate is available" in:
        val busy1 = Mockito.mock(classOf[Robot])
        when(busy1.canAccept).thenReturn(false)
        
        val busyRobots = Iterable((busy1, Position(1, 1)))

        val result = selector.choose(mission, busyRobots)

        result shouldBe None
