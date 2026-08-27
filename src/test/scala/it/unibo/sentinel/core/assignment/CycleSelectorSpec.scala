package it.unibo.sentinel.core.assignment

import it.unibo.sentinel.UnitTest
import it.unibo.sentinel.core.mission.*
import it.unibo.sentinel.core.warehouse.Position
import it.unibo.sentinel.core.scenario.Placement

class CycleSelectorSpec extends UnitTest with SelectorBehaviors:

  private val mission = Mission(MissionId("M01"), Task.goto(Position(0, 0)), 10)

  private def createCandidates(): (Placement, Placement, Placement) =
    (
      Placement(mockRobot(canAccept = true), Position(1, 1)),
      Placement(mockRobot(canAccept = true), Position(2, 2)),
      Placement(mockRobot(canAccept = true), Position(3, 3))
    )

  "A CycleSelector" when:

    behave like commonSelector(Selector.CycleSelector())

    "assigning missions sequentially" should:

      "first cycle through unused candidates" in:
        val (p1, p2, _) = createCandidates()
        val selector = Selector.CycleSelector()

        selector.choose(mission, Iterable(p1, p2)) shouldBe Some(p1)
        selector.choose(mission, Iterable(p1, p2)) shouldBe Some(p2)

      "rotate back to the least recently used candidate once all have been used" in:
        val (p1, p2, _) = createCandidates()
        val candidates = Iterable(p1, p2)
        val selector = Selector.CycleSelector()

        selector.choose(mission, candidates)
        selector.choose(mission, candidates)

        selector.choose(mission, candidates) shouldBe Some(p1)
        selector.choose(mission, candidates) shouldBe Some(p2)

      "skip candidates that are unavailable when cycling" in:
        val (p1, p2, _) = createCandidates()
        val selector = Selector.CycleSelector()

        selector.choose(mission, Iterable(p1, p2))
        selector.choose(mission, Iterable(p1, p2))

        selector.choose(mission, Iterable(p2)) shouldBe Some(p2)

      "preserve priority of a candidate when it becomes available again after being busy" in:
        val (p1, p2, p3) = createCandidates()
        val selector = Selector.CycleSelector()
        val all = Iterable(p1, p2, p3)
        val withoutP1 = Iterable(p2, p3)

        selector.choose(mission, all) shouldBe Some(p1)
        selector.choose(mission, all) shouldBe Some(p2)

        selector.choose(mission, withoutP1) shouldBe Some(p3)

        selector.choose(mission, all) shouldBe Some(p1)
