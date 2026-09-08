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

/** The history of the simulation as a sequence of pairs of events and the tick
  * at which they occurred.
  */
type History = Vector[(Event, Tick)]

/** The identifier of a [[Simulation]].
  */
opaque type SimulationId = String

object SimulationId:
  /** @param id
    *   a [[String]] representing the identifier of a [[Simulation]].
    * @return
    *   a [[SimulationId]].
    */
  def apply(id: String): SimulationId = id

  extension (id: SimulationId)
    /** @return
      *   the identifier as a [[String]].
      */
    def value: String = id

/** Represents the discrete-time simulation of a scenario. It is responsible for
  * keeping track of the current time and for executing the actions of the
  * scenario at each tick.
  */
trait Simulation:
  /** @return
    *   the ID of the simulation.
    */
  def id: SimulationId

  /** @return
    *   the current time of the simulation
    */
  def time: Tick

  /** @return
    *   a [[Snapshot]] of the simulation at the current time.
    */
  def snapshot: Snapshot

  /** Advances the simulation by one tick.
    */
  def step(): StepResult

  /** @return
    *   whether the simulation is over.
    */
  def isOver: Boolean

  /** @return
    *   the [[History]] of the [[Simulation]] so far.
    */
  def history: History

  /** Returns statistics captured from the current history and executed time.
    */
  def statistics: Statistics.Report

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
    given SelectionPolicy =
      scenario.collisionSelection()(using scenario.missions)
    given CollisionHandler = scenario.collisionAvoidance()
    fromWorld(scenario.build)

  /** @param scenario
    *   the [[Scenario]] to simulate.
    * @return
    *   a [[Simulation]] of the given [[Scenario]] that ends when all the
    *   missions are over.
    */
  def of(id: SimulationId, scenario: Scenario): Simulation =
    withContext(scenario): world =>
      new BasicSimulation(id, scenario, world, Phase.all)

  /** @param scenario
    *   the [[Scenario]] to simulate.
    * @param limit
    *   the limit of the simulation, in [[Tick]]s.
    * @return
    *   a [[Simulation]] of the given [[Scenario]] that ends when all the
    *   [[Mission]]s are or when the limit is reached.
    */
  def of(id: SimulationId, scenario: Scenario, limit: Tick): Simulation =
    withContext(scenario): world =>
      new BasicSimulation(id, scenario, world, Phase.all) with TimeLimit(limit)

  private abstract class AbstractSimulation extends Simulation:

    private var recorded: History = Vector.empty

    protected final def recordEvents(events: Seq[Event], tick: Tick): Unit =
      recorded = recorded ++ (for event <- events yield (event, tick))

    def history: History = recorded

    def world: Environment

  private class BasicSimulation(
      val id: SimulationId,
      scenario: Scenario,
      val world: Environment,
      phases: Seq[Phase]
  ) extends AbstractSimulation:
    private var currentTime: Tick = Tick(0)

    def time: Tick = currentTime

    def snapshot: Snapshot = world.snapshot

    def statistics: Statistics.Report =
      Statistics.report(scenario, history, time)

    def step(): StepResult =
      val events = phases.flatMap(_.apply(world))
      recordEvents(events, currentTime)
      currentTime = currentTime.next
      StepResult(snapshot = world.snapshot, events = events)

    def isOver: Boolean = world.missions.forall(_.isOver)

  private trait TimeLimit(max: Tick) extends AbstractSimulation:
    private def limitReached: Boolean =
      summon[Ordering[Tick]].gteq(time, max)

    abstract override def step(): StepResult =
      val now = time
      val stepResult = super.step()
      if limitReached
      then
        val lastEvents = world.end
        recordEvents(lastEvents, now)
        StepResult(
          snapshot = world.snapshot,
          events = stepResult.events ++ lastEvents
        )
      else stepResult

    abstract override def isOver: Boolean =
      super.isOver || limitReached
