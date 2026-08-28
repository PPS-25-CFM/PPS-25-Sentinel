package it.unibo.sentinel.core.assignment

import org.scalatestplus.mockito.MockitoSugar
import org.mockito.Mockito.when
import it.unibo.sentinel.UnitTest
import it.unibo.sentinel.core.mission.*
import it.unibo.sentinel.core.robot.Robot
import it.unibo.sentinel.core.warehouse.Position
import it.unibo.sentinel.core.scenario.Placement

trait SelectorBehaviors extends MockitoSugar:
  this: UnitTest =>

  def commonSelector(selectorBuilder: => Selector): Unit =
    val mission = Mission(MissionId("M01"), Task.goto(Position(0, 0)), 10)

    "asked to assign a mission" should:

      "return None if no candidate is present" in:
        selectorBuilder.choose(mission, Iterable.empty) shouldBe None

      "return None if every candidate is busy" in:
        val busyPlacement =
          Placement(mockRobot(canAccept = false), Position(0, 0))
        selectorBuilder.choose(mission, Iterable(busyPlacement)) shouldBe None

  protected def mockRobot(canAccept: Boolean): Robot =
    val robot = mock[Robot]
    when(robot.canAccept).thenReturn(canAccept)
    robot
