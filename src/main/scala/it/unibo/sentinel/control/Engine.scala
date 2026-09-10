package it.unibo.sentinel.control

import it.unibo.sentinel.core.simulation.{Simulation, StepResult, Tick}
import it.unibo.sentinel.core.simulation.Statistics.Report
import monix.eval.Task
import monix.execution.Scheduler
import monix.reactive.Observable
import scala.concurrent.duration.FiniteDuration

/** Advances a simulation periodically and hands each result to an observer.
  */
trait Engine:

  /** Describes a complete run of the [[Simulation]].
    * @param onStep
    *   The observer evaluated after every simulation [[StepResult]].
    * @return
    *   A [[Task]] producing the [[Report]] of the completed [[Simulation]].
    */
  def run(onStep: StepResult => Task[Unit]): Task[Report]

object Engine:

  /** The commands that can be used to control the [[Simulation]].
    */
  enum Command:
    /** Pauses the [[Simulation]].
      */
    case Pause

    /** Resumes the [[Simulation]].
      */
    case Resume

    /** Goes back in the [[Simulation]] and pauses it.
      */
    case Back

    /** Goes forward in the [[Simulation]] and pauses it.
      */
    case Next

  /** Creates an [[Engine]] that advances the given [[Simulation]] every
    * [[period]].
    *
    * @param simulation
    *   The [[Simulation]] to advance.
    * @param period
    *   The time interval between simulation steps.
    * @param commands
    *   The [[Command]]s driving the `simulation` while it runs.
    * @return
    *   An [[Engine]] that advances the given simulation.
    */
  def apply(
      simulation: Simulation,
      period: FiniteDuration,
      commands: Observable[Command]
  )(using Scheduler): Engine =
    new ReactiveEngine(simulation) with ControllableClock(commands, period)

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

  private trait ControllableClock(
      commands: Observable[Command],
      period: FiniteDuration
  ):
    self: ReactiveEngine =>
    import Command.*, Movement.*

    override def clock: Observable[Tick] =
      (Observable.now(Resume) ++ commands)
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

  private enum Movement:
    case Keep, Backward, Forward
