package it.unibo.sentinel.control

import it.unibo.sentinel.UnitTest
import it.unibo.sentinel.control.Engine.BasicEngine
import it.unibo.sentinel.core.simulation.{
  Simulation,
  Snapshot,
  StepResult,
  Tick
}
import it.unibo.sentinel.core.warehouse.Warehouse
import monix.execution.Scheduler
import monix.execution.schedulers.TestScheduler

import scala.concurrent.duration.*

class BasicEngineSpec extends UnitTest:

  private final class StubSimulation(stepLimit: Int) extends Simulation:
    private var completedSteps = 0
    override def time: Tick = Tick(completedSteps)

    override def step(): StepResult =
      completedSteps += 1
      StepResult(
        Snapshot(Warehouse.empty(completedSteps, 1), Seq.empty, Seq.empty),
        Seq.empty
      )

    override def isOver: Boolean = completedSteps >= stepLimit

    def stepCount: Int = completedSteps

  "A BasicEngine" when:

    "not started" should:

      "leave the simulation idle" in:
        val scheduler = TestScheduler()
        given Scheduler = scheduler
        val period = 1.second
        val simulation = StubSimulation(stepLimit = 1)
        val engine = BasicEngine(simulation, period)
        var observedSteps = 0
        val _ = engine.observe(_ => observedSteps += 1)
        scheduler.tick()
        simulation.stepCount shouldBe 0
        observedSteps shouldBe 0

    "started" should:

      "advance the simulation and notify its observers" in:
        val scheduler = TestScheduler()
        given Scheduler = scheduler

        val simulation = StubSimulation(stepLimit = 1)
        val engine = BasicEngine(simulation, 1.second)
        var observedSteps = 0
        val _ = engine.observe(_ => observedSteps += 1)
        val _ = engine.start()
        scheduler.tick()
        simulation.stepCount shouldBe 1
        observedSteps shouldBe 1

      "share each simulation step among all observers" in:
        val scheduler = TestScheduler()
        given Scheduler = scheduler

        val simulation = StubSimulation(stepLimit = 1)
        val engine = BasicEngine(simulation, 1.second)
        var counter1, counter2 = 0
        val _ = engine.observe(_ => counter1 += 1)
        val _ = engine.observe(_ => counter2 += 1)
        val _ = engine.start()
        scheduler.tick()
        simulation.stepCount shouldBe 1
        counter1 shouldBe 1
        counter2 shouldBe 1

    "remove a canceled observer without stopping the engine" in:
      val scheduler = TestScheduler()
      given Scheduler = scheduler

      val period = 1.second
      val simulation = StubSimulation(stepLimit = Int.MaxValue)
      val engine = BasicEngine(simulation, period)
      var counter1, counter2 = 0
      val obs1 = engine.observe(_ => counter1 += 1)
      val _ = engine.observe(_ => counter2 += 1)
      val _ = engine.start()
      scheduler.tick()
      obs1.stop()
      scheduler.tick(period)
      simulation.stepCount shouldBe 2
      counter1 shouldBe 1
      counter2 shouldBe 2

    "stop advancing when it is stopped" in:
      val scheduler = TestScheduler()
      given Scheduler = scheduler

      val period = 1.second
      val sim = StubSimulation(stepLimit = Int.MaxValue)
      val engine = BasicEngine(sim, period)
      var counter = 0
      val _ = engine.observe(_ => counter += 1)
      val cancelable = engine.start()
      scheduler.tick()
      cancelable.stop()
      scheduler.tick(period)
      sim.stepCount shouldBe 1
      counter shouldBe 1

    "stop automatically when the simulation is over" in:
      val scheduler = TestScheduler()
      given Scheduler = scheduler

      val period = 1.second
      val simulation = StubSimulation(stepLimit = 1)
      val engine = BasicEngine(simulation, period)
      var counter = 0
      val _ = engine.observe(_ => counter += 1)
      val _ = engine.start()
      scheduler.tick(period * 2)
      simulation.stepCount shouldBe 1
      counter shouldBe 1

    "controlled" should:

      "not react to commands" in:
        val scheduler = TestScheduler()
        given Scheduler = scheduler
        val period = 1.second
        val simulation = StubSimulation(stepLimit = 1)
        val engine = BasicEngine(simulation, period)
        val _ = engine.start()
        engine.next()
        engine.back()
        engine.resume()
        engine.pause()
        scheduler.tick()
        simulation.stepCount shouldBe 1