package it.unibo.sentinel.core.simulation

import it.unibo.sentinel.UnitTest
import it.unibo.sentinel.core.TestData

class SimulationSpec extends UnitTest with TestData with EnvironmentFixture:
  "A Simulation" when:

    "created" should:
      val sim = Simulation.of(emptyScenario)

      "start at tick zero" in:
        sim.time shouldBe Tick(0)

    "stepped" should:
      val sim = Simulation.of(scenario)

      "increment the time by one tick" in:
        sim.step()
        sim.time shouldBe Tick(1)

      "return a step result with no events if nothing happens" in:
        val sim = Simulation.of(emptyScenario)
        val result = sim.step()
        result.events shouldBe empty

      "collect all the events that happened during the step" in:
        val step = sim.step()
        step.events should contain(Event.MissionAssigned(r1, m1))
        step.events should contain(Event.RobotRouted(r1, Seq(p3)))
        step.events should contain(Event.RobotMoved(r1, p1, p3))
