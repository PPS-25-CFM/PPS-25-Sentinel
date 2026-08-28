package it.unibo.sentinel.core.simulation

import it.unibo.sentinel.UnitTest

import it.unibo.sentinel.core.simulation.Simulation

class WithLimitSpec extends UnitTest with SimulationBehaviours:
  "A WithLimit simulation" when:

    behave like commonSimulation { scenario =>
      Simulation.of(scenario, limit = Tick(Int.MaxValue))
    }

    "when the limit is reached" should:
      val sim = Simulation.of(scenario, limit = Tick(1))

      "be over" in:
        val _ = sim.step()
        sim.isOver shouldBe true
