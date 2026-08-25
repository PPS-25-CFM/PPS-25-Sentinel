package it.unibo.sentinel.core

import org.mockito.Mockito
import org.mockito.Mockito.*
import org.scalatest.OneInstancePerTest
import it.unibo.sentinel.UnitTest
import it.unibo.sentinel.core.mission.*
import it.unibo.sentinel.core.robot.Robot
import it.unibo.sentinel.core.warehouse.Position
import it.unibo.sentinel.core.assignment.*
import it.unibo.sentinel.core.assignment.Selector.* 
import it.unibo.sentinel.core.routing.Navigator
import it.unibo.sentinel.core.scenario.Placement

trait SelectorBehaviors:
  this: UnitTest =>

  def commonSelector(selectorBuilder: => Selector): Unit =
    val mission = Mission(MissionId("M01"), Task.goto(Position(0, 0)), 10)

    "asked to assign a mission" should:

      "return None if no candidate is present" in:
        selectorBuilder.choose(mission, Iterable.empty) shouldBe None

      "return None if every candidate is busy" in:
        val busyPlacement = Placement(mockRobot(canAccept = false), Position(0, 0))
        selectorBuilder.choose(mission, Iterable(busyPlacement)) shouldBe None

  protected def mockRobot(canAccept: Boolean): Robot =
    val robot = Mockito.mock(classOf[Robot])
    when(robot.canAccept).thenReturn(canAccept)
    robot

class SelectorSpec extends UnitTest with SelectorBehaviors with OneInstancePerTest:

  // Grazie a OneInstancePerTest, questi campi vengono ri-creati da zero per OGNI test!
  val targetPosition = Position(0, 0)
  val mission = Mission(MissionId("M01"), Task.goto(targetPosition), 10)
  val destination = mission.currentDestination.value

  val placement1 = Placement(mockRobot(canAccept = true), Position(1, 1))
  val placement2 = Placement(mockRobot(canAccept = true), Position(2, 2))
  val placement3 = Placement(mockRobot(canAccept = true), Position(3, 3))

  val candidates = Iterable(placement1, placement2)
  val navigator = Mockito.mock(classOf[Navigator])

  "A Nearest Selector" when:

    behave like commonSelector(Selector.Nearest(navigator))

    "evaluating proximity" should:

      "return the placement of the nearest available robot to the mission position" in:
        when(navigator.distance(placement1.at, destination)).thenReturn(Some(2))
        when(navigator.distance(placement2.at, destination)).thenReturn(Some(4))

        Selector.Nearest(navigator).choose(mission, candidates) shouldBe Some(placement1)

      "return None if all candidates are unreachable" in:
        when(navigator.distance(placement1.at, destination)).thenReturn(None)
        when(navigator.distance(placement2.at, destination)).thenReturn(None)

        Selector.Nearest(navigator).choose(mission, candidates) shouldBe None

      "ignore candidates it cannot reach" in:
        val stranded = Placement(mockRobot(canAccept = true), Position(3, 3))
        when(navigator.distance(stranded.at, destination)).thenReturn(None)
        when(navigator.distance(placement2.at, destination)).thenReturn(Some(4))

        Selector.Nearest(navigator).choose(mission, Iterable(stranded, placement2)) shouldBe Some(placement2)

      "ignore when the mission has no destination" in:
        Selector.Nearest(navigator).choose(mission.complete, candidates) shouldBe None

  "A CycleSelector" when:

    behave like commonSelector(CycleSelector())

    "assigning missions sequentially" should:

      "first cycle through unused candidates" in:
        val cycleSelector = CycleSelector()

        cycleSelector.choose(mission, candidates) shouldBe Some(placement1)
        cycleSelector.choose(mission, candidates) shouldBe Some(placement2)

      "rotate back to the least recently used candidate once all have been used" in:
        val cycleSelector = CycleSelector()

        cycleSelector.choose(mission, candidates)
        cycleSelector.choose(mission, candidates)

        cycleSelector.choose(mission, candidates) shouldBe Some(placement1)
        cycleSelector.choose(mission, candidates) shouldBe Some(placement2)

      "skip candidates that are unavailable when cycling" in:
        val cycleSelector = CycleSelector()

        cycleSelector.choose(mission, candidates)
        cycleSelector.choose(mission, candidates)

        cycleSelector.choose(mission, Iterable(placement2)) shouldBe Some(placement2)

      "preserve priority of a candidate when it becomes available again after being busy" in:
        val cycleSelector = CycleSelector()
        val allCandidates = Iterable(placement1, placement2, placement3)
        val candidatesWithoutPlacement1 = Iterable(placement2, placement3)

        cycleSelector.choose(mission, allCandidates) shouldBe Some(placement1)
        cycleSelector.choose(mission, allCandidates) shouldBe Some(placement2)

        cycleSelector.choose(mission, candidatesWithoutPlacement1) shouldBe Some(placement3)

        cycleSelector.choose(mission, allCandidates) shouldBe Some(placement1)