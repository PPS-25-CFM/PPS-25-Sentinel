package it.unibo.sentinel.control

import it.unibo.sentinel.UnitTest
import it.unibo.sentinel.core.simulation.Simulation
import monix.execution.Scheduler
import monix.execution.schedulers.TestScheduler
import org.mockito.Mockito.*
import scala.concurrent.duration.*

trait EngineSpecBehaviour extends UnitTest:

  def createEngine(simulation: Simulation, period: FiniteDuration)(using
      Scheduler
  ): Engine

  protected trait EngineFixture:
    val scheduler = TestScheduler()
    given Scheduler = scheduler
    val period = 1.second
    val simulation = mock[Simulation]()
    val engine = createEngine(simulation, period)

  "An Engine" when:

    "not started" should:
      "leave the simulation idle" in new EngineFixture:
        var observedSteps = 0
        engine.observe(_ => observedSteps += 1)
        scheduler.tick()
        observedSteps shouldBe 0

    "started" should:
      "advance the simulation and notify its observers" in new EngineFixture:
        var c = 0
        engine.observe(_ => c += 1)
        engine.start()
        scheduler.tick()
        verify(simulation).step()
        c shouldBe 1

      "share each simulation step among all observers" in new EngineFixture:
        var c1 = 0
        var c2 = 0
        engine.observe(_ => c1 += 1)
        engine.observe(_ => c2 += 1)
        engine.start()
        scheduler.tick()
        verify(simulation).step()
        c1 shouldBe 1
        c2 shouldBe 1

      "remove a canceled observer without stopping the engine" in new EngineFixture:
        var c1 = 0
        var c2 = 0
        val obs1 = engine.observe(_ => c1 += 1)
        engine.observe(_ => c2 += 1)
        engine.start()
        scheduler.tick()
        obs1.stop()
        scheduler.tick(period)
        verify(simulation, times(2)).step()
        c1 shouldBe 1
        c2 shouldBe 2

      "stop advancing when it is stopped" in new EngineFixture:
        var c = 0
        engine.observe(_ => c += 1)
        val cancelable = engine.start()
        scheduler.tick()
        cancelable.stop()
        scheduler.tick(period)
        verify(simulation, times(1)).step()
        c shouldBe 1

      "stop automatically when the simulation is over" in new EngineFixture:
        when(simulation.isOver).thenReturn(false, false, true)
        var c = 0
        engine.observe(_ => c += 1)
        engine.start()
        scheduler.tick(period * 2)
        c shouldBe 1
