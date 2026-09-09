package it.unibo.sentinel.boundary.launcher

import it.unibo.sentinel.control.{Engine, Stoppable}
import it.unibo.sentinel.core.simulation.StepResult
import it.unibo.sentinel.core.simulation.Statistics.Report

/** Owns an engine's execution and subscriptions.
  *
  * Call start and stop on the UI thread; execute must dispatch engine events
  * onto that same thread.
  */
final class SimulationSession(
    engine: Engine,
    execute: (() => Unit) => Unit
) extends Stoppable:

  private enum State:
    case Ready, Active, Closed

  private var state = State.Ready
  private var resources = List.empty[Stoppable]

  /** Subscribes before starting the engine.
    *
    * A session can only start once.
    */
  def start(
      onStep: StepResult => Unit,
      onCompleted: Report => Unit
  ): Unit =
    if state == State.Ready then
      state = State.Active

      track(engine.observe(step => deliver(onStep(step))))

      track(
        engine.observeCompletion(report =>
          deliver {
            stop()
            onCompleted(report)
          }
        )
      )

      track(engine.start())

  /** Cancels execution and subscriptions, discarding pending events.
    */
  override def stop(): Unit =
    state = State.Closed

    val previous = resources
    resources = List.empty
    previous.foreach(_.stop())

  private def deliver(action: => Unit): Unit =
    execute(() => if state == State.Active then action)

  /** Handles closure during registration, before the handle is returned. */
  private def track(resource: => Stoppable): Unit =
    if state == State.Active then
      val handle = resource
      if state == State.Active then resources = handle :: resources
      else handle.stop()
