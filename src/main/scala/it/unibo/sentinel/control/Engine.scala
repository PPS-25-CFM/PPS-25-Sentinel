package it.unibo.sentinel.control

import it.unibo.sentinel.core.simulation.{Simulation, StepResult, Tick}
import monix.execution.{Cancelable, Scheduler}
import monix.reactive.Observable
import scala.Conversion
import scala.concurrent.duration.FiniteDuration
import scala.language.implicitConversions
import monix.reactive.subjects.ConcurrentSubject

/** Represents something that can be stopped.
  */
trait Stoppable:
  /** Stops the underlying process.
    */
  def stop(): Unit

trait Controller:
  /** Pauses the [[Simulation]].
    */
  def pause(): Unit

  /** Resumes the [[Simulation]].
    */
  def resume(): Unit

  /** Moves the [[Simulation]] one step back.
    */
  def back(): Unit

  /** Moves the [[Simulation]] one step forward.
    */
  def next(): Unit

/** Advances a simulation periodically and publishes each result to observers.
  */
trait Engine extends Controller:

  /** Registers a callback invoked after every simulation step.
    */
  def observe(onStep: StepResult => Unit): Stoppable

  /** Starts the simulation.
    */
  def start(): Stoppable

object Engine:
  /** Creates an [[Engine]] that advances the given [[Simulation]] every
    * [[period]] and notifies its observers after each step.
    *
    * @param simulation
    *   The simulation to advance.
    * @param period
    *   The time interval between simulation steps.
    * @return
    *   An [[Engine]] that advances the given simulation.
    */
  def apply(
      simulation: Simulation,
      period: FiniteDuration
  ): Engine =
    given Scheduler = Scheduler.singleThread("engine")
    new ReactiveEngine(simulation) with ControllableClock(period)

  private[control] abstract class ReactiveEngine(val simulation: Simulation)(
      using Scheduler
  ) extends Engine:
    def clock: Observable[Tick]

    private val history: LazyList[StepResult] =
      LazyList.unfold(()): _ =>
        Option.unless(simulation.isOver)((simulation.step(), ()))

    private lazy val steps =
      clock
        .collect:
          case Tick(time) if history.isDefinedAt(time) => history(time)
        .publish

    override def observe(onStep: StepResult => Unit): Stoppable =
      steps.foreach(onStep)

    override def start(): Stoppable =
      steps.connect()

    private given Conversion[Cancelable, Stoppable] with
      override def apply(source: Cancelable): Stoppable =
        () => source.cancel()

  private[control] trait ControllableClock(period: FiniteDuration)(using
      Scheduler
  ):
    self: ReactiveEngine =>
    import ControlledClock.*, Command.*, Movement.*

    private val commands = ConcurrentSubject.publish[Command]

    override def clock: Observable[Tick] =
      commands
        .startWith(Seq(Resume))
        .switchMap:
          case Pause  => Observable.now(Keep)
          case Back   => Observable.now(Backward)
          case Next   => Observable.now(Forward)
          case Resume =>
            Observable
              .interval(period)
              .map(i => if i == 0 then Keep else Forward)
        .scan(Tick.zero):
          case (time, Keep)     => time
          case (time, Backward) => time.previous
          case (time, Forward)  => time.next
        .takeWhile(_ => !simulation.isOver)

    override def pause(): Unit = submit(Pause)

    override def resume(): Unit = submit(Resume)

    override def back(): Unit = submit(Back)

    override def next(): Unit = submit(Next)

    private def submit(command: Command): Unit = commands.onNext(command)

  private[control] object ControlledClock:
    enum Command:
      case Pause, Resume, Back, Next

    enum Movement:
      case Keep, Backward, Forward
