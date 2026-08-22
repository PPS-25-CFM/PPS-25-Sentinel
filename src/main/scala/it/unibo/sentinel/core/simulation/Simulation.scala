package it.unibo.sentinel.core.simulation

import it.unibo.sentinel.core.scenario.Scenario

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

object Simulation:
  /** @param scenario
    *   the scenario to simulate.
    * @return
    *   a simulation of the given scenario that ends when all the missions are
    *   completed or failed.
    */
  def of(scenario: Scenario): Simulation =
    // given Warehouse = scenario.warehouse
    // given Navigator = scenario.routing()
    // given Selector = scenario.assignment()
    val world = scenario.build
    BasicSimulation(world)

  private final class BasicSimulation(world: Environment) extends Simulation:
    val _ = world
    private var currentTime: Tick = Tick(0)
    def time: Tick = currentTime
    def step(): StepResult =
      currentTime = currentTime.next
      StepResult(snapshot = world.snapshot, events = Seq.empty)
