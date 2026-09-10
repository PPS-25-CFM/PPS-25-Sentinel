package it.unibo.sentinel.control

import it.unibo.sentinel.core.simulation.{Simulation, StepResult, Tick}
import it.unibo.sentinel.core.simulation.Statistics.Report
import monix.eval.Task
import monix.execution.Scheduler
import monix.reactive.Observable
import monix.reactive.subjects.ConcurrentSubject
import scala.concurrent.duration.FiniteDuration

trait Controller:
  /** Pauses the [[Simulation]].
    */
  def pause(): Unit

  /** Resumes the [[Simulation]].
    */
  def resume(): Unit

  /** Moves the [[Simulation]] one step back and pauses it.
    */
  def back(): Unit

  /** Moves the [[Simulation]] one step forward and pauses it.
    */
  def next(): Unit

/** Advances a simulation periodically and hands each result to an observer.
  */
trait Engine extends Controller:

  /** Describes a complete run of the [[Simulation]].
    * @param onStep
    *   The observer evaluated after every simulation [[StepResult]].
    * @return
    *   A [[Task]] producing the [[Report]] of the completed [[Simulation]].
    */
  def run(onStep: StepResult => Task[Unit]): Task[Report]

object Engine:
  /** Creates an [[Engine]] that advances the given [[Simulation]] every
    * [[period]].
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
  )(using Scheduler): Engine =
    new ReactiveEngine(simulation) with ControllableClock(period)

  private abstract class ReactiveEngine(simulation: Simulation)(using
      scheduler: Scheduler
  ) extends Engine:
    def clock: Observable[Tick]

    private val history: LazyList[StepResult] =
      val initial = StepResult(simulation.snapshot, Seq.empty)
      LazyList
        .iterate(initial)(_ => simulation.step())
        .takeWhile(_ => !simulation.isOver)

    override def run(onStep: StepResult => Task[Unit]): Task[Report] =
      clock
        .map { case Tick(time) => history.lift(time) }
        .takeWhileInclusive(_ => !simulation.isOver)
        .collect { case Some(step) => step }
        .mapEval(onStep)
        .completedL
        .map(_ => simulation.statistics)
        .executeOn(scheduler)

  private trait ControllableClock(period: FiniteDuration)(using
      Scheduler
  ):
    self: ReactiveEngine =>
    import ControlledClock.*, Command.*, Movement.*

    /** Retains the latest command, so that one submitted before the run has
      * subscribed still drives the clock.
      */
    private val commands = ConcurrentSubject.behavior[Command](Resume)

    override def clock: Observable[Tick] =
      commands
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

    override def pause(): Unit = submit(Pause)

    override def resume(): Unit = submit(Resume)

    override def back(): Unit = submit(Back)

    override def next(): Unit = submit(Next)

    private def submit(command: Command): Unit = commands.onNext(command)

  private object ControlledClock:
    enum Command:
      case Pause, Resume, Back, Next

    enum Movement:
      case Keep, Backward, Forward
