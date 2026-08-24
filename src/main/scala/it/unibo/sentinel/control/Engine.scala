package it.unibo.sentinel.control

import it.unibo.sentinel.core.simulation.{Simulation, StepResult}
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

  private class BasicEngine(simulation: Simulation, period: FiniteDuration)(
      using Scheduler
  ) extends Engine:

    private given Conversion[Cancelable, Stoppable] with
      override def apply(source: Cancelable): Stoppable =
        () => source.cancel()

    protected val clock = Observable.interval(period)

    private val steps =
      clock
        .takeWhile(_ => !simulation.isOver)
        .map(_ => simulation.step())
        .publish

    override def observe(onStep: StepResult => Unit): Stoppable =
      steps.foreach(onStep)

    override def start(): Stoppable =
      steps.connect()
