package it.unibo.sentinel.core.simulation

import it.unibo.sentinel.UnitTest

class BasicSimulationSpec extends UnitTest with SimulationBehaviours:
  "A BasicSimulation" when:

    val id = SimulationId("sim-test")

    behave like commonSimulation(Simulation.of(id, _))

    "when there are still missions" should:
      val id = SimulationId("sim-test")
      val sim = Simulation.of(id, scenario)
      "not be over" in:
        sim.isOver shouldBe false
