package it.unibo.sentinel.core.simulation

import it.unibo.sentinel.core.scenario.Scenario

/** Represents the discrete-time simulation of a scenario. It is responsible for
  * keeping track of the current time and for executing the actions of the
  * scenario at each tick.
  */
trait Simulation:
  /** @return
    *   the current time of the simulation
    */
  def time: Tick

object Simulation:
  /** @param scenario
    *   the scenario to simulate.
    * @return
    *   a simulation of the given scenario that ends when all the missions are
    *   completed or failed.
    */
  def of(scenario: Scenario): Simulation = new Simulation:
    val _ = scenario
    def time: Tick = Tick(0)
