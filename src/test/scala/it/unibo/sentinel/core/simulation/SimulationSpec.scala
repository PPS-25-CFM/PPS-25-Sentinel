package it.unibo.sentinel.core.simulation

import it.unibo.sentinel.UnitTest
import it.unibo.sentinel.core.TestData
import it.unibo.sentinel.core.robot.RobotStatus
import it.unibo.sentinel.core.mission.MissionStatus

class SimulationSpec extends UnitTest with TestData with EnvironmentFixture:
  "A Simulation" when:

    "created" should:
      val sim = Simulation.of(emptyScenario)

      "start at tick zero" in:
        sim.time shouldBe Tick(0)

    "stepped" should:

      "increment the time by one tick" in:
        val sim = Simulation.of(scenario)
        sim.step()
        sim.time shouldBe Tick(1)

      "return a step result with no events if nothing happens" in:
        val sim = Simulation.of(emptyScenario)
        val result = sim.step()
        result.events shouldBe empty

      "collect all the events that happened during the step" in:
        val sim = Simulation.of(scenario)
        val step = sim.step()
        step.events should contain(Event.MissionAssigned(r1, m1))
        step.events should contain(Event.RobotRouted(r1, Seq(p3)))
        step.events should contain(Event.RobotMoved(r1, p1, p3))
        step.events should contain(Event.MissionCompleted(m1))

      "return a snapshot of the environment after the step" in:
        val sim = Simulation.of(scenario)
        val step = sim.step()
        val snapshot = step.snapshot
        snapshot.robots should contain(RobotSnapshot(r1, RobotStatus.Idle, p3))
        val completed = snapshot.missions.find(_.id == m1).value
        completed.status shouldBe MissionStatus.Completed
        completed.duration shouldBe deadline - 1
