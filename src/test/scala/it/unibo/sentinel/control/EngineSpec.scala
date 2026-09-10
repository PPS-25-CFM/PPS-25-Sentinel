package it.unibo.sentinel.control

import it.unibo.sentinel.UnitTest
import it.unibo.sentinel.control.Engine.Command
import monix.eval.Task
import monix.execution.Scheduler
import monix.execution.schedulers.TestScheduler
import monix.reactive.subjects.ConcurrentSubject
import org.mockito.Mockito.*
import scala.concurrent.Promise
import scala.concurrent.duration.*
import scala.util.Success
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
    val commands = ConcurrentSubject.publish[Command]
    val simulation = mock[Simulation]()
    val initial = StepResult(mock[Snapshot](), Seq.empty)
    val second = StepResult(mock[Snapshot](), Seq.empty)
    val expectedReport = mock[Statistics.Report]()
    when(simulation.snapshot).thenReturn(initial.snapshot)
    when(simulation.step()).thenReturn(second)
    when(simulation.statistics).thenReturn(expectedReport)
    val engine = Engine(simulation, period, commands)

    def submit(command: Command): Unit =
      val _ = commands.onNext(command)

  "An Engine" when:

    "not run" should:
      "leave the simulation idle" in new EngineFixture:
        var step = Option.empty[StepResult]
        val _ = engine.run(result => Task { step = Some(result) })
        scheduler.tick(period * 2)
        verify(simulation, never()).step()
        step shouldBe None

    "run" should:

      "show the initial state of the simulation" in new EngineFixture:
        var step = Option.empty[StepResult]
        val _ = engine.run(result => Task { step = Some(result) }).runToFuture
        scheduler.tick()
        step shouldBe Some(initial)
        verify(simulation, never()).step()

      "advance the simulation and notify its observer" in new EngineFixture:
        var step = Option.empty[StepResult]
        val _ =
          engine.run(result => Task { step = Some(result) }).runToFuture
        scheduler.tick(period)
        step shouldBe Some(second)
        verify(simulation, times(1)).step()

      "ignore the commands submitted before it starts" in new EngineFixture:
        var step = Option.empty[StepResult]
        submit(Command.Next)
        scheduler.tick()
        val _ = engine.run(result => Task { step = Some(result) }).runToFuture
        scheduler.tick()
        step shouldBe Some(initial)
        verify(simulation, never()).step()

      "stop advancing when it is canceled" in new EngineFixture:
        var step = Option.empty[StepResult]
        val cancellable =
          engine.run(result => Task { step = Some(result) }).runToFuture
        scheduler.tick()
        cancellable.cancel()
        scheduler.tick(period)
        verify(simulation, never()).step()
        step shouldBe Some(initial)

      "not advance while the observer task is pending" in new EngineFixture:
        val gate = Promise[Unit]()
        val _ = engine.run(_ => Task.fromFuture(gate.future)).runToFuture
        scheduler.tick(period * 3)
        verify(simulation, never()).step()

      "advance again once the observer task completes" in new EngineFixture:
        val gate = Promise[Unit]()
        val _ = engine.run(_ => Task.fromFuture(gate.future)).runToFuture
        scheduler.tick(period * 3)
        val _ = gate.success(())
        scheduler.tick(period)
        verify(simulation, times(1)).step()

    "paused" should:
      "stop advancing the simulation" in new EngineFixture:
        var step = Option.empty[StepResult]
        val _ = engine.run(result => Task { step = Some(result) }).runToFuture
        scheduler.tick()
        submit(Command.Pause)
        scheduler.tick(period)
        step shouldBe Some(initial)
        verify(simulation, never()).step()

    "resumed" should:
      "resume advancing the simulation" in new EngineFixture:
        var step = Option.empty[StepResult]
        val _ = engine.run(result => Task { step = Some(result) }).runToFuture
        scheduler.tick()
        submit(Command.Pause)
        scheduler.tick(period)
        submit(Command.Resume)
        scheduler.tick(period)
        step shouldBe Some(second)
        verify(simulation, times(1)).step()

    "moved one step back" should:

      "move the simulation one step back" in new EngineFixture:
        var step = Option.empty[StepResult]
        val _ = engine.run(result => Task { step = Some(result) }).runToFuture
        scheduler.tick(period)
        step shouldBe Some(second)
        submit(Command.Back)
        scheduler.tick()
        step shouldBe Some(initial)

      "pause the simulation" in new EngineFixture:
        var step = Option.empty[StepResult]
        val _ = engine.run(result => Task { step = Some(result) }).runToFuture
        scheduler.tick(period)
        submit(Command.Back)
        scheduler.tick(2 * period)
        step shouldBe Some(initial)
        verify(simulation, times(1)).step()

    "moved one step forward" should:

      "move the simulation one step forward" in new EngineFixture:
        var step = Option.empty[StepResult]
        val _ = engine.run(result => Task { step = Some(result) }).runToFuture
        scheduler.tick()
        submit(Command.Next)
        scheduler.tick()
        step shouldBe Some(second)

      "pause the simulation" in new EngineFixture:
        var step = Option.empty[StepResult]
        val _ = engine.run(result => Task { step = Some(result) }).runToFuture
        scheduler.tick()
        submit(Command.Next)
        scheduler.tick(period)
        step shouldBe Some(second)
        verify(simulation, times(1)).step()
        scheduler.tick(period * 2)
        verify(simulation, times(1)).step()

    "terminated" should:
      "produce the report of the completed simulation" in new EngineFixture:
        when(simulation.isOver).thenReturn(false, true)
        val running = engine.run(_ => Task.unit).runToFuture
        scheduler.tick(period)
        running.value shouldBe Some(Success(expectedReport))
