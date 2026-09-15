package it.unibo.sentinel.control

import it.unibo.sentinel.UnitTest
import it.unibo.sentinel.control.Engine.Command
import it.unibo.sentinel.core.simulation.Tick
import monix.execution.Scheduler
import monix.execution.schedulers.TestScheduler
import monix.reactive.subjects.ConcurrentSubject
import scala.concurrent.duration.*

trait ClockFixture:
  val scheduler = TestScheduler()
  given Scheduler = scheduler
  val period = 1.second
  val commands = ConcurrentSubject.publish[Command]
  val timer: Engine.Timer = new Engine.ControllableClock(commands, period) {}
  var ticks = Seq.empty[Tick]
  def start(): Unit =
    timer.clock.foreach(tick => ticks = ticks :+ tick)
  def submit(command: Command): Unit =
    commands.onNext(command)
  val t0 = Tick.zero
  val t1 = Tick(1)
  val t2 = Tick(2)
  val t3 = Tick(3)

class ControllableClockSpec extends UnitTest:
  import Command.*

  "A ControllableClock" when:

    "started" should:

      "emit the zero tick immediately" in new ClockFixture:
        start()
        scheduler.tick()
        ticks shouldBe Seq(t0)

      "not advance before a period has elapsed" in new ClockFixture:
        start()
        scheduler.tick(period - 1.millis)
        ticks shouldBe Seq(t0)

      "advance by one tick every period" in new ClockFixture:
        start()
        scheduler.tick(period * 3)
        ticks shouldBe Seq(t0, t1, t2, t3)

    "paused" should:

      "stop advancing" in new ClockFixture:
        start()
        scheduler.tick(period)
        submit(Pause)
        scheduler.tick(period * 3)
        ticks shouldBe Seq(t0, t1)

    "resumed" should:

      "advance again every period" in new ClockFixture:
        start()
        scheduler.tick()
        submit(Pause)
        scheduler.tick(period * 3)
        submit(Resume)
        scheduler.tick(period * 2)
        ticks shouldBe Seq(t0, t1, t2)

    "moved one step back" should:

      "emit the previous tick and stop advancing" in new ClockFixture:
        start()
        scheduler.tick(period * 2)
        submit(Back)
        scheduler.tick(period * 3)
        ticks shouldBe Seq(t0, t1, t2, t1)

      "not go before the zero tick" in new ClockFixture:
        start()
        scheduler.tick()
        submit(Back)
        scheduler.tick(period)
        ticks shouldBe Seq(t0)

    "moved one step forward" should:

      "emit the next tick and stop advancing" in new ClockFixture:
        start()
        scheduler.tick()
        submit(Next)
        scheduler.tick(period * 3)
        ticks shouldBe Seq(t0, t1)
