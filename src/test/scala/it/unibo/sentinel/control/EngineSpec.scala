package it.unibo.sentinel.control

import it.unibo.sentinel.UnitTest
import monix.execution.Scheduler
import monix.execution.schedulers.TestScheduler
import org.mockito.Mockito.*
import scala.concurrent.duration.*
import it.unibo.sentinel.control.Engine.ReactiveEngine
import it.unibo.sentinel.control.Engine.ControllableClock
import it.unibo.sentinel.core.simulation.{
  Simulation,
  Statistics,
  StepResult,
  Snapshot
}

class EngineSpec extends UnitTest:

  protected trait EngineFixture:
    val scheduler = TestScheduler()
    given Scheduler = scheduler
    val period = 1.second
    val simulation = mock[Simulation]()
    val initial = StepResult(mock[Snapshot](), Seq.empty)
    val second = StepResult(mock[Snapshot](), Seq.empty)
    val expectedReport = mock[Statistics.Report]()
    when(simulation.snapshot).thenReturn(initial.snapshot)
    when(simulation.step()).thenReturn(second)
    when(simulation.statistics).thenReturn(expectedReport)
    val engine = new ReactiveEngine(simulation) with ControllableClock(period)

  "An Engine" when:

    "not started" should:
      "leave the simulation idle" in new EngineFixture:
        scheduler.tick()
        verify(simulation, never()).step()

    "started" should:

      "show the initial state of the simulation" in new EngineFixture:
        var step = Option.empty[StepResult]
        engine.observe(r => step = Some(r))
        engine.start()
        scheduler.tick()
        step shouldBe Some(initial)
        verify(simulation, never()).step()

      "advance the simulation and notify its observers" in new EngineFixture:
        var step = Option.empty[StepResult]
        engine.observe(r => step = Some(r))
        engine.start()
        scheduler.tick(period)
        step shouldBe Some(second)
        verify(simulation, times(1)).step()

      "share each simulation step among all observers" in new EngineFixture:
        var step1 = Option.empty[StepResult]
        var step2 = Option.empty[StepResult]
        engine.observe(r => step1 = Some(r))
        engine.observe(r => step2 = Some(r))
        engine.start()
        scheduler.tick()
        step1 shouldBe Some(initial)
        step2 shouldBe Some(initial)

      "remove a canceled observer without stopping the engine" in new EngineFixture:
        var step1 = Option.empty[StepResult]
        var step2 = Option.empty[StepResult]
        val obs1 = engine.observe(r => step1 = Some(r))
        engine.observe(r => step2 = Some(r))
        engine.start()
        scheduler.tick()
        obs1.stop()
        scheduler.tick(period)
        verify(simulation, times(1)).step()
        step1 shouldBe Some(initial)
        step2 shouldBe Some(second)

      "stop advancing when it is stopped" in new EngineFixture:
        var step = Option.empty[StepResult]
        engine.observe(r => step = Some(r))
        val cancelable = engine.start()
        scheduler.tick()
        cancelable.stop()
        scheduler.tick(period)
        verify(simulation, never()).step()
        step shouldBe Some(initial)

      "stop automatically when the simulation is over" in new EngineFixture:
        when(simulation.isOver).thenReturn(false, false, true)
        engine.start()
        scheduler.tick(period * 3)
        verify(simulation, times(1)).step()

    "paused" should:
      "stop advancing the simulation" in new EngineFixture:
        engine.start()
        scheduler.tick()
        engine.pause()
        scheduler.tick(period)
        verify(simulation, never()).step()

    "resumed" should:
      "resume advancing the simulation" in new EngineFixture:
        engine.start()
        scheduler.tick()
        engine.pause()
        scheduler.tick(period)
        engine.resume()
        scheduler.tick(period)
        verify(simulation, times(1)).step()

    "moved one step back" should:

      "move the simulation one step back" in new EngineFixture:
        var step = Option.empty[StepResult]
        engine.observe(r => step = Some(r))
        engine.start()
        scheduler.tick(period)
        step shouldBe Some(second)
        engine.back()
        scheduler.tick()
        step shouldBe Some(initial)

      "pause the simulation" in new EngineFixture:
        engine.start()
        scheduler.tick(period)
        engine.back()
        scheduler.tick(2 * period)
        verify(simulation, times(1)).step()

    "moved one step forward" should:

      "move the simulation one step forward" in new EngineFixture:
        var step = Option.empty[StepResult]
        engine.observe(r => step = Some(r))
        engine.start()
        engine.next()
        scheduler.tick()
        step shouldBe Some(second)

      "pause the simulation" in new EngineFixture:
        engine.start()
        engine.next()
        scheduler.tick(period)
        verify(simulation, times(1)).step()
        scheduler.tick(period * 2)
        verify(simulation, times(1)).step()

    "terminated" should:
      "provide a report to its observers" in new EngineFixture:
        var report: Option[Statistics.Report] = None
        engine.observeCompletion(r => report = Some(r))
        when(simulation.isOver).thenReturn(false, true)
        engine.start()
        scheduler.tick(period)
        report shouldBe defined
