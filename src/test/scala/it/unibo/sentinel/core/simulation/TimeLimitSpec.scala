package it.unibo.sentinel.core.simulation

import it.unibo.sentinel.UnitTest

import it.unibo.sentinel.core.simulation.Simulation
import it.unibo.sentinel.core.robot.RobotStatus

class TimeLimitSpec extends UnitTest with SimulationBehaviours:
  "A TimeLimit simulation" when:

    behave like commonSimulation { scenario =>
      Simulation.of(scenario, limit = Tick(Int.MaxValue))
    }

    "when the limit is reached" should:

      "be over" in:
        val sim = Simulation.of(scenario, limit = Tick(1))
        val _ = sim.step()
        sim.isOver shouldBe true

      "fail all pending missions" in:
        val sim = Simulation.of(scenario, limit = Tick(1))
        val stepResult = sim.step()
        stepResult.events should contain allOf (
          Event.MissionFailed(m1),
          Event.MissionFailed(m2)
        )

      "release all robots" in:
        val sim = Simulation.of(scenario, limit = Tick(1))
        val stepResult = sim.step()
        val robots = stepResult.snapshot.robots
        all(robots.map(_.status)) shouldBe RobotStatus.Idle
