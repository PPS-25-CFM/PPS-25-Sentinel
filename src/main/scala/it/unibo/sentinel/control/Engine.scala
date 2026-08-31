package it.unibo.sentinel.control

import it.unibo.sentinel.core.simulation.{Simulation, StepResult, Tick}
import monix.execution.{Cancelable, Scheduler}
import monix.reactive.Observable
import scala.Conversion
import scala.concurrent.duration.FiniteDuration
import scala.language.implicitConversions

/** Represents something that can be stopped.
  */
trait Stoppable:
  /** Stops the underlying process.
    */
  def stop(): Unit

/** Advances a simulation periodically and publishes each result to observers.
  */
trait Engine:

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
  )(using Scheduler): Engine =
    BasicEngine(simulation, period)

  private[control] abstract class ReactiveEngine(simulation: Simulation)(using
      Scheduler
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

  private[control] class BasicEngine(
      simulation: Simulation,
      period: FiniteDuration
  )(using
      Scheduler
  ) extends ReactiveEngine(simulation):
    override def clock: Observable[Tick] =
      Observable
        .interval(period)
        .takeWhile(_ => !simulation.isOver)
        .map(_.toInt)
        .map(Tick(_))
