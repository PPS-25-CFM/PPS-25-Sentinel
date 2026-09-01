package it.unibo.sentinel.control

import it.unibo.sentinel.UnitTest
import it.unibo.sentinel.core.simulation.Simulation
import monix.execution.Scheduler
import monix.execution.schedulers.TestScheduler
import org.mockito.Mockito.*
import scala.concurrent.duration.*
import it.unibo.sentinel.control.Engine.ReactiveEngine
import it.unibo.sentinel.control.Engine.ControllableClock

class EngineSpecBehaviour extends UnitTest:

  protected trait EngineFixture:
    val scheduler = TestScheduler()
    given Scheduler = scheduler
    val period = 1.second
    val simulation = mock[Simulation]()
    val engine = new ReactiveEngine(simulation) with ControllableClock(period)

  "An Engine" when:

    "not started" should:
      "leave the simulation idle" in new EngineFixture:
        scheduler.tick()
        verify(simulation, never()).step()

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
        engine.start()
        scheduler.tick(period * 2)
        verify(simulation, times(1)).step()

    "paused" should:
      "stop advancing the simulation" in new EngineFixture:
        engine.start()
        scheduler.tick()
        engine.pause()
        scheduler.tick(period)
        verify(simulation, times(1)).step()

    "resumed" should:
      "resume advancing the simulation" in new EngineFixture:
        engine.start()
        scheduler.tick()
        engine.pause()
        scheduler.tick(period)
        engine.resume()
        scheduler.tick(period)
        verify(simulation, times(2)).step()

    "moved one step back" should:

      "move the simulation one step back" in new EngineFixture:
        engine.start()
        scheduler.tick()
        engine.back()
        scheduler.tick(period)
        verify(simulation, times(1)).step()

      "pause the simulation" in new EngineFixture:
        engine.start()
        scheduler.tick()
        engine.back()
        scheduler.tick(period)
        verify(simulation, times(1)).step()
        scheduler.tick(period * 2)
        verify(simulation, times(1)).step()

    "moved one step forward" should:

      "move the simulation one step forward" in new EngineFixture:
        engine.start()
        scheduler.tick()
        engine.next()
        scheduler.tick(period)
        verify(simulation, times(2)).step()

      "pause the simulation" in new EngineFixture:
        engine.start()
        scheduler.tick()
        engine.next()
        scheduler.tick(period)
        verify(simulation, times(2)).step()
        scheduler.tick(period * 2)
        verify(simulation, times(2)).step()
