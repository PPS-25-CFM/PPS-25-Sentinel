package it.unibo.sentinel.core.assignment

import org.mockito.Mockito.when
import it.unibo.sentinel.UnitTest
import it.unibo.sentinel.core.item.Item
import it.unibo.sentinel.core.mission.*
import it.unibo.sentinel.core.warehouse.{Position, Warehouse}
import it.unibo.sentinel.core.routing.{Navigator, Path, Step}
import it.unibo.sentinel.core.scenario.Placement
import it.unibo.sentinel.core.simulation.Tick

class NearestSelectorSpec extends UnitTest with SelectorBehaviors:

  private val targetPosition = Position(0, 0)
  private val mission = Mission.relocate(
    MissionId("M01"),
    targetPosition,
    Tick(10)
  )
  private val destination = mission.currentTarget.value

  "A Nearest Selector" when:
    val navigator = mock[Navigator]
    val selector = Selector.Nearest(navigator)

    behave like commonSelector(selector)

    "evaluating proximity" should:

      "return the placement of the nearest available robot to the mission position" in:
        val p1 = Placement(mockRobot(canAccept = true), Position(1, 1))
        val p2 = Placement(mockRobot(canAccept = true), Position(2, 2))

        when(navigator.distance(p1.at, destination)).thenReturn(Some(2))
        when(navigator.distance(p2.at, destination)).thenReturn(Some(4))

        selector.choose(mission, Iterable(p1, p2)) shouldBe Some(p1)

      "return None if all candidates are unreachable" in:
        val p1 = Placement(mockRobot(canAccept = true), Position(1, 1))
        val p2 = Placement(mockRobot(canAccept = true), Position(2, 2))

        when(navigator.distance(p1.at, destination)).thenReturn(None)
        when(navigator.distance(p2.at, destination)).thenReturn(None)

        selector.choose(mission, Iterable(p1, p2)) shouldBe None

      "ignore candidates it cannot reach" in:
        val p2 = Placement(mockRobot(canAccept = true), Position(2, 2))
        val stranded = Placement(mockRobot(canAccept = true), Position(3, 3))

        when(navigator.distance(stranded.at, destination)).thenReturn(None)
        when(navigator.distance(p2.at, destination)).thenReturn(Some(4))

        selector.choose(mission, Iterable(stranded, p2)) shouldBe Some(p2)

      "ignore when the mission has no destination" in:
        val p1 = Placement(mockRobot(canAccept = true), Position(1, 1))

        selector.choose(mission.complete, Iterable(p1)) shouldBe None

      "return the nearest to the shelf interaction points for a PickUp" in:
        val shelf = Position(2, 2)
        val bay = Position(3, 3)
        val deliver = Mission.deliver(
          MissionId("D1"),
          Item.Computer,
          shelf,
          bay,
          Tick(10)
        )

        val mockWh = mock[Warehouse]
        val ips = Seq(Position(1, 2), Position(2, 1))
        when(mockWh.interactionPoints(shelf)).thenReturn(ips)
        when(navigator.warehouse).thenReturn(mockWh)

        val p1 = Placement(mockRobot(canAccept = true), Position(1, 1))
        val p2 = Placement(mockRobot(canAccept = true), Position(4, 4))
        when(navigator.path(p1.at, ips.toSet))
          .thenReturn(Some(Path(Step(Position(1, 2), Tick.unit))))
        when(navigator.path(p2.at, ips.toSet)).thenReturn(None)

        selector.choose(deliver, Iterable(p1, p2)) shouldBe Some(p1)
