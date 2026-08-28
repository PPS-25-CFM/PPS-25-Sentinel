package it.unibo.sentinel.core.simulation

import it.unibo.sentinel.UnitTest

class BasicSimulationSpec extends UnitTest with SimulationBehaviours:
  "A BasicSimulation" when:

    behave like commonSimulation(Simulation.of)

    "when there are still missions" should:
      val sim = Simulation.of(scenario)
      "not be over" in:
        sim.isOver shouldBe false
