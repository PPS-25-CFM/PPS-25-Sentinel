package it.unibo.sentinel.core

import org.mockito.Mockito
import org.mockito.Mockito.*
import it.unibo.sentinel.UnitTest
import it.unibo.sentinel.core.mission.*
import it.unibo.sentinel.core.robot.{Robot, RobotId}
import it.unibo.sentinel.core.simulation.Tick
import it.unibo.sentinel.core.warehouse.Position
import it.unibo.sentinel.core.assignment.*
import it.unibo.sentinel.core.routing.Navigator
import it.unibo.sentinel.core.scenario.Placement

/** Shared behavior contract for any Selector implementation
  */
trait SelectorBehaviors:
  this: UnitTest =>

  def commonSelector(selectorBuilder: => Selector): Unit =
    val mission = Mission.relocate(
      id = MissionId("M01"),
      destination = Position(0, 0),
      duration = Tick(10)
    )

    "asked to assign a mission" should:

      "return None if no candidate is present" in:
        val result = selectorBuilder.choose(mission, Iterable.empty[Placement])

        result shouldBe None

      "return None if every candidate is busy" in:
        val busyRobot = Mockito.mock(classOf[Robot])
        when(busyRobot.canAccept).thenReturn(false)
        val busyPlacement = Placement(busyRobot, Position(0, 0))

        val result = selectorBuilder.choose(mission, Iterable(busyPlacement))

        result shouldBe None

      "return None if no candidate can take more work" in:
        val fullRobot = Mockito.mock(classOf[Robot])
        when(fullRobot.canAccept).thenReturn(false)
        val fullPlacement = Placement(fullRobot, Position(0, 0))

        val result = selectorBuilder.choose(mission, Iterable(fullPlacement))

        result shouldBe None

class SelectorSpec extends UnitTest with SelectorBehaviors:

  val targetPosition = Position(0, 0)
  val mission =
    Mission.relocate(
      id = MissionId("M01"),
      destination = targetPosition,
      duration = Tick(10)
    )

  "A Nearest Selector" when:
    val robot1 = Mockito.mock(classOf[Robot])
    when(robot1.canAccept).thenReturn(true)
    val placement1 = Placement(robot1, Position(1, 1))

    val robot2 = Mockito.mock(classOf[Robot])
    when(robot2.canAccept).thenReturn(true)
    val placement2 = Placement(robot2, Position(2, 2))

    val placements = Iterable(placement1, placement2)

    val navigator = Mockito.mock(classOf[Navigator])
    val destination = mission.currentTarget.value

    when(navigator.distance(placement1.at, destination)).thenReturn(Some(2))
    when(navigator.distance(placement2.at, destination)).thenReturn(Some(4))

    val selector = Selector.Nearest(navigator)

    behave like commonSelector(selector)

    "evaluating proximity" should:

      "return the placement of the nearest available robot to the mission position" in:
        val result = selector.choose(mission, placements)

        result shouldBe Some(placement1)

      "ignore when the mission has no destination" in:
        val completedMission = mission.assignTo(RobotId("R0")).complete

        selector.choose(completedMission, placements) shouldBe None

      "ignore candidates it cannot reach" in:
        val strandedRobot = Mockito.mock(classOf[Robot])
        when(strandedRobot.canAccept).thenReturn(true)
        val strandedPlacement = Placement(strandedRobot, Position(3, 3))

        when(navigator.distance(strandedPlacement.at, destination))
          .thenReturn(None)

        val result =
          selector.choose(mission, Iterable(strandedPlacement, placement2))

        result shouldBe Some(placement2)
