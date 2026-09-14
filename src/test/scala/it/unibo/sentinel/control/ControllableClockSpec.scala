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

class ControllableClockSpec extends UnitTest:
  import Command.*

  "A ControllableClock" when:

    "started" should:

      "emit the zero tick immediately" in new ClockFixture:
        start()
        scheduler.tick()
        ticks shouldBe Seq(Tick.zero)

      "not advance before a period has elapsed" in new ClockFixture:
        start()
        scheduler.tick(period - 1.millis)
        ticks shouldBe Seq(Tick.zero)

      "advance by one tick every period" in new ClockFixture:
        start()
        scheduler.tick(period * 3)
        ticks shouldBe Seq(Tick.zero, Tick(1), Tick(2), Tick(3))

    "paused" should:

      "repeat the current tick and stop advancing" in new ClockFixture:
        start()
        scheduler.tick(period)
        submit(Pause)
        scheduler.tick(period * 3)
        ticks shouldBe Seq(Tick.zero, Tick(1))

    "resumed" should:
      "advance again every period" in new ClockFixture:
        start()
        scheduler.tick()
        submit(Pause)
        scheduler.tick(period * 3)
        submit(Resume)
        scheduler.tick(period * 2)
        ticks shouldBe Seq(Tick.zero, Tick(1), Tick(2))

    "moved one step back" should:

      "emit the previous tick and stop advancing" in new ClockFixture:
        start()
        scheduler.tick(period * 2)
        submit(Back)
        scheduler.tick(period * 3)
        ticks shouldBe Seq(Tick.zero, Tick(1), Tick(2), Tick(1))

      "not go before the zero tick" in new ClockFixture:
        start()
        scheduler.tick()
        submit(Back)
        scheduler.tick(period)
        ticks shouldBe Seq(Tick.zero)

    "moved one step forward" should:
      "emit the next tick and stop advancing" in new ClockFixture:
        start()
        scheduler.tick()
        submit(Next)
        scheduler.tick(period * 3)
        ticks shouldBe Seq(Tick.zero, Tick(1))
