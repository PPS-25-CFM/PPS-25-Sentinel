package it.unibo.sentinel.core.simulation

import it.unibo.sentinel.UnitTest
import it.unibo.sentinel.core.TestData
import it.unibo.sentinel.core.scenario.Scenario
import it.unibo.sentinel.core.robot.RobotStatus
import it.unibo.sentinel.core.mission.MissionStatus

trait SimulationBehaviours extends TestData with EnvironmentFixture:
  self: UnitTest =>

  def commonSimulation(build: Scenario => Simulation): Unit =
    "created" should:
      val sim = build(emptyScenario)

      "start at tick zero" in:
        sim.time shouldBe Tick.zero

    "stepped" should:

      "increment the time by one tick" in:
        val sim = build(scenario)
        val t1 = Tick(1)
        sim.step()
        sim.time shouldBe t1

      "return a step result at the time reached after the step" in:
        val sim = build(scenario)
        val t1 = Tick(1)
        val t2 = Tick(2)
        val step1 = sim.step()
        step1.at shouldBe t1
        val step2 = sim.step()
        step2.at shouldBe t2

      "return a step result with no events if nothing happens" in:
        val sim = build(emptyScenario)
        val result = sim.step()
        result.events shouldBe empty

      "collect all the events that happened during the step" in:
        val sim = build(scenario)
        val step1 = sim.step()
        step1.events should contain(Event.MissionAssigned(r1, m1))
        step1.events should contain(Event.RobotRouted(r1, Seq(p3)))
        val step2 = sim.step()
        step2.events should contain(Event.RobotMoved(r1, p1, p3))
        step2.events should contain(Event.MissionCompleted(m1))

      "return a snapshot of the environment after the step" in:
        val sim = build(scenario)

        val step1 = sim.step()
        val snapshot1 = step1.snapshot

        val robotInStep1 = snapshot1.robots.find(_.id == r1).value
        robotInStep1.status shouldBe RobotStatus.Moving
        robotInStep1.position shouldBe p1
        robotInStep1.path shouldBe defined

        val step2 = sim.step()
        val snapshot2 = step2.snapshot

        val robotInStep2 = snapshot2.robots.find(_.id == r1).value
        robotInStep2.status shouldBe RobotStatus.Idle
        robotInStep2.position shouldBe p3
        robotInStep2.path shouldBe empty

        val completed = snapshot2.missions.find(_.id == m1).value
        completed.status shouldBe MissionStatus.Completed
        completed.deadline shouldBe deadline - 2

    "when all missions are completed" should:
      val sim = build(emptyScenario)
      "be over" in:
        sim.isOver shouldBe true
