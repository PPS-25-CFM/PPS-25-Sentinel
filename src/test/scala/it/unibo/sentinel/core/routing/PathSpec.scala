package it.unibo.sentinel.core.routing

import it.unibo.sentinel.UnitTest
import it.unibo.sentinel.core.warehouse.Position
import it.unibo.sentinel.core.simulation.Tick

class PathSpec extends UnitTest:
  "A Path" when:

    "empty" should:
      val emptyPath: Path = Path.empty

      "lead to no position" in:
        emptyPath.positions shouldBe Seq.empty

      "have no remaining time to wait" in:
        emptyPath.remaining shouldBe Tick.zero

    "made of a single step" should:
      val destination = Position(1, 0)
      val cost = Tick(1)
      val path: Path = Path(Step(destination, cost))

      "lead to the destination of that step" in:
        path.positions shouldBe Seq(destination)

      "have the cost of that step as remaining time" in:
        path.remaining shouldBe cost

    "made of several steps" should:
      val steps = Seq(
        Step(Position(1, 0), Tick.unit),
        Step(Position(2, 0), Tick(2)),
        Step(Position(3, 0), Tick(3))
      )
      val path: Path = Path(steps*)

      "have the cost of its first step as remaining time" in:
        path.remaining shouldBe steps.headOption.value.cost

    "advanced" should:

      "be empty if it was the last step and remaining time is up" in:
        val path: Path = Path(Step(Position(1, 0), Tick.zero))
        path.advanced shouldBe Path.empty

      "be the same if the remaining time is not up" in:
        val path: Path = Path(Step(Position(1, 0), Tick.unit))
        path.advanced shouldBe path

      "drop the first step if the remaining time is up" in:
        val path: Path = Path(
          Step(Position(1, 0), Tick.zero),
          Step(Position(2, 0), Tick.unit)
        )
        val next = path.advanced
        next.advanced shouldBe Path(Step(Position(2, 0), Tick.unit))
        next.remaining shouldBe Tick.unit

    "ticked" should:

      "decrease the remaining time of the first step by one tick" in:
        val path: Path = Path(Step(Position(1, 0), Tick.unit))
        path.ticked.remaining shouldBe Tick.zero
