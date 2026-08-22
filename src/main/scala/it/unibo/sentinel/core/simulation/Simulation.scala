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

  /** Advances the simulation by one tick.
    */
  def step(): Unit

object Simulation:
  /** @param scenario
    *   the scenario to simulate.
    * @return
    *   a simulation of the given scenario that ends when all the missions are
    *   completed or failed.
    */
  def of(scenario: Scenario): Simulation = new Simulation:
    val _ = scenario
    private var currentTime: Tick = Tick(0)
    def time: Tick = currentTime
    def step(): Unit = currentTime = currentTime.next
