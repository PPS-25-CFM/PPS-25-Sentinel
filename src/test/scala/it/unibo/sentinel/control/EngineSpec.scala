package it.unibo.sentinel.control

import it.unibo.sentinel.UnitTest
import monix.eval.Task
import monix.execution.Scheduler
import monix.execution.schedulers.TestScheduler
import monix.reactive.Observable
import monix.reactive.subjects.ConcurrentSubject
import org.mockito.Mockito.*
import scala.concurrent.Promise
import scala.util.Success
import it.unibo.sentinel.core.simulation.{
  Simulation,
  Statistics,
  StepResult,
  Snapshot,
  Tick
}

trait EngineFixture:
  val scheduler = TestScheduler()
  given Scheduler = scheduler
  val simulation = mock[Simulation]()
  val initial = StepResult(Tick.zero, mock[Snapshot](), Seq.empty)
  val second = StepResult(Tick(1), mock[Snapshot](), Seq.empty)
  val expectedReport = mock[Statistics.Report]()
  when(simulation.snapshot).thenReturn(initial.snapshot)
  when(simulation.step()).thenReturn(second)
  when(simulation.statistics).thenReturn(expectedReport)
  var notified = Seq.empty[StepResult]
  val record: StepObserver = step => Task { notified = notified :+ step }
  val t0 = Tick.zero
  val t1 = Tick(1)
  def engineOn(ticks: Observable[Tick]): Engine =
    new Engine.ReactiveEngine(simulation) with Engine.Timer:
      override def clock: Observable[Tick] = ticks

class EngineSpec extends UnitTest:
  "An Engine" when:

    "not run" should:

      "leave the simulation idle" in new EngineFixture:
        val clock = Observable(t0, t1)
        val _ = engineOn(clock).run(record)
        scheduler.tick()
        notified shouldBe empty
        verify(simulation, never()).step()

    "run" should:

      "notify the initial state at the zero tick" in new EngineFixture:
        val clock = Observable(t0)
        val _ = engineOn(clock).run(record).runToFuture
        scheduler.tick()
        notified shouldBe Seq(initial)
        verify(simulation, never()).step()

      "advance the simulation at the next tick" in new EngineFixture:
        val clock = Observable(t0, t1)
        val _ =
          engineOn(clock).run(record).runToFuture
        scheduler.tick()
        notified shouldBe Seq(initial, second)
        verify(simulation, times(1)).step()

      "replay the steps already computed" in new EngineFixture:
        val clock = Observable(t0, t1, t0)
        val _ = engineOn(clock).run(record).runToFuture
        scheduler.tick()
        notified shouldBe Seq(initial, second, initial)
        verify(simulation, times(1)).step()

      "notify the same step again when the tick repeats" in new EngineFixture:
        val clock = Observable(t0, t0)
        val _ = engineOn(clock).run(record).runToFuture
        scheduler.tick()
        notified shouldBe Seq(initial, initial)
        verify(simulation, never()).step()

      "not consume further ticks while the observer task is pending" in new EngineFixture:
        val gate = Promise[Unit]()
        val clock = Observable(t0, t1)
        val _ =
          engineOn(clock).run(_ => Task.fromFuture(gate.future)).runToFuture
        scheduler.tick()
        verify(simulation, never()).step()

      "consume the next tick once the observer task completes" in new EngineFixture:
        val gate = Promise[Unit]()
        val clock = Observable(t0, t1)
        val _ =
          engineOn(clock).run(_ => Task.fromFuture(gate.future)).runToFuture
        scheduler.tick()
        val _ = gate.success(())
        scheduler.tick()
        verify(simulation, times(1)).step()

      "stop advancing when it is canceled" in new EngineFixture:
        val clock = ConcurrentSubject.publish[Tick]
        val running = engineOn(clock).run(record).runToFuture
        scheduler.tick()
        val _ = clock.onNext(t0)
        scheduler.tick()
        running.cancel()
        val _ = clock.onNext(t1)
        scheduler.tick()
        notified shouldBe Seq(initial)
        verify(simulation, never()).step()

    "terminated" should:

      "produce the report of the completed simulation" in new EngineFixture:
        when(simulation.isOver).thenReturn(false, true)
        val clock = Observable.now(t0) ++ Observable.never
        val running = engineOn(clock).run(record).runToFuture
        scheduler.tick()
        running.value shouldBe Some(Success(expectedReport))
