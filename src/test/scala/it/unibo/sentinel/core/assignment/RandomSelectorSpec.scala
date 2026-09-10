package it.unibo.sentinel.core.assignment

import it.unibo.sentinel.UnitTest
import it.unibo.sentinel.core.mission.*
import it.unibo.sentinel.core.routing.Navigator
import it.unibo.sentinel.core.scenario.{Placement, Policies}
import it.unibo.sentinel.core.simulation.Tick
import it.unibo.sentinel.core.warehouse.Position
import org.mockito.Mockito.when
import scala.util.Random

class RandomSelectorSpec extends UnitTest with SelectorBehaviors:

  private val mission =
    Mission.relocate(MissionId("M01"), Position(0, 0), Tick(10))

  private def createCandidates(): (Placement, Placement, Placement) =
    (
      Placement(mockRobot(canAccept = true), Position(1, 1)),
      Placement(mockRobot(canAccept = true), Position(2, 2)),
      Placement(mockRobot(canAccept = true), Position(3, 3))
    )

  "A RandomSelector" when:

    behave like commonSelector(Selector.RandomSelector(mock[Random]))

    "selecting among available candidates" should:

      "always return the only available candidate" in:
        val (p1, _, _) = createCandidates()

        val rng = mock[Random]
        when(rng.nextInt(1)).thenReturn(0)

        val selector = Selector.RandomSelector(rng)

        selector.choose(mission, Iterable(p1)) shouldBe Some(p1)

      "return exactly the candidate at the index extracted by the random generator" in:
        val (p1, p2, p3) = createCandidates()
        val candidates = Vector(p1, p2, p3)

        for drawnIndex <- candidates.indices do
          val rng = mock[Random]
          when(rng.nextInt(candidates.size)).thenReturn(drawnIndex)

          val selector = Selector.RandomSelector(rng)
          selector.choose(mission, candidates) shouldBe Some(
            candidates(drawnIndex)
          )

    "resolving a policy" should:

      "build a RandomSelector from Policies.Assignment.Random" in:
        given Navigator = mock[Navigator]
        val policy: Policies.Assignment = Policies.Assignment.Random

        policy(new Random(0)) shouldBe a[Selector.RandomSelector]
