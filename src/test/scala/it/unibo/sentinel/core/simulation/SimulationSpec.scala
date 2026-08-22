package it.unibo.sentinel.core.simulation

import it.unibo.sentinel.UnitTest
import it.unibo.sentinel.core.TestData

class SimulationSpec extends UnitTest with TestData:
  "A Simulation" when:
    "created" should:
      val sim = Simulation.of(emptyScenario)
      "start at tick zero" in:
        sim.time shouldBe Tick(0)
