package it.unibo.sentinel.core.selector

import it.unibo.sentinel.UnitTest
import it.unibo.sentinel.core.robot.*
import it.unibo.sentinel.core.warehouse.Position
import it.unibo.sentinel.core.mission.*
import it.unibo.sentinel.core.selection.Placement
import it.unibo.sentinel.core.selection.Selector.*
import it.unibo.sentinel.core.routing.Navigator

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
  
  val navigator = Mockito.mock(classOf[Navigator])
  
  val selector = NearestSelector(navigator)

  "A Selector" when:

    "asked to assign a mission" should:

      "return None if no candidate is present" in:
        val result = selector.choose(
          mission,
          Iterable.empty[Placement]
        )

        result shouldBe None

      "return None if no candidate is available" in:
        val busy1 = Mockito.mock(classOf[Robot])
        when(busy1.canAccept).thenReturn(false)

        val placement = Placement(busy1, Position(1, 1))        
        val busyRobots = Iterable(placement)

        val result = selector.choose(mission, busyRobots)

        result shouldBe None

    "evaluating" should:
      val robot1 = Robot(RobotId("Robot1"))
      val robot2 = Robot(RobotId("Robot2"))
      val placement1 = Placement(robot1, Position(1, 1))
      val placement2 = Placement(robot2, Position(2, 2))

      val robots = Iterable(placement1, placement2)

      "return the available robot to the mission position" in:

        val result = selector.choose(mission, robots)

        result shouldBe Some(placement1)
