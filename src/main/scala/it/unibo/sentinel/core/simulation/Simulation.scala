package it.unibo.sentinel.core.simulation

import it.unibo.sentinel.core.scenario.Scenario
import it.unibo.sentinel.core.routing.Navigator
import it.unibo.sentinel.core.assignment.Selector
import it.unibo.sentinel.core.warehouse.Warehouse
import it.unibo.sentinel.core.collisions.SelectionPolicy
import it.unibo.sentinel.core.collisions.CollisionHandler

/** @param snapshot
  *   the snapshot of the simulation after the step.
  * @param events
  *   the events that occurred during the step.
  */
final case class StepResult(snapshot: Snapshot, events: Seq[Event])

/** Represents the discrete-time simulation of a scenario. It is responsible for
  * keeping track of the current time and for executing the actions of the
  * scenario at each tick.
  */
trait Simulation:
  /** @return
    *   the current time of the simulation
    */
  def time: Tick

  /** Advances the simulation by one tick.
    */
  def step(): StepResult

  /** @return
    *   whether the simulation is over.
    */
  def isOver: Boolean

object Simulation:

  private def withContext(scenario: Scenario)(
      fromWorld: (
          Warehouse,
          Navigator,
          Selector,
          SelectionPolicy,
          CollisionHandler
      ) ?=> Environment => Simulation
  ): Simulation =
    given Warehouse = scenario.warehouse
    given Navigator = scenario.routing()
    given Selector = scenario.assignment()
    given SelectionPolicy = scenario.collisionSelection()
    given CollisionHandler = scenario.collisionAvoidance()
    fromWorld(scenario.build)

  /** @param scenario
    *   the [[Scenario]] to simulate.
    * @return
    *   a [[Simulation]] of the given [[Scenario]] that ends when all the
    *   missions are over.
    */
  def of(scenario: Scenario): Simulation =
    withContext(scenario): world =>
      BasicSimulation(world, Phase.all)

  /** @param scenario
    *   the [[Scenario]] to simulate.
    * @param limit
    *   the limit of the simulation, in [[Tick]]s.
    * @return
    *   a [[Simulation]] of the given [[Scenario]] that ends when all the
    *   [[Mission]]s are or when the limit is reached.
    */
  def of(scenario: Scenario, limit: Tick): Simulation =
    withContext(scenario): world =>
      new BasicSimulation(world, Phase.all) with TimeLimit(limit)

  private abstract class AbstractSimulation extends Simulation:
    protected def world: Environment

  private class BasicSimulation(
      val world: Environment,
      phases: Seq[Phase]
  ) extends AbstractSimulation:
    private var currentTime: Tick = Tick(0)

    def time: Tick = currentTime

    def step(): StepResult =
      val events = phases.flatMap(_.apply(world))
      currentTime = currentTime.next
      StepResult(snapshot = world.snapshot, events = events)

    def isOver: Boolean = world.missions.forall(_.isOver)

  private trait TimeLimit(max: Tick) extends AbstractSimulation:
    private def limitReached: Boolean =
      summon[Ordering[Tick]].gteq(time, max)

    abstract override def step(): StepResult =
      val stepResult = super.step()
      if limitReached
      then
        val lastEvents = world.end
        StepResult(
          snapshot = world.snapshot,
          events = stepResult.events ++ lastEvents
        )
      else stepResult

    abstract override def isOver: Boolean =
      super.isOver || limitReached
