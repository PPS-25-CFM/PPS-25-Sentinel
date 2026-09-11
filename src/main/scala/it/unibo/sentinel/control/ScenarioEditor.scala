package it.unibo.sentinel.control

import it.unibo.sentinel.core.scenario.Scenario
import it.unibo.sentinel.core.warehouse.Position
import it.unibo.sentinel.core.scenario.RobotClass
import it.unibo.sentinel.core.scenario.Validation
import it.unibo.sentinel.core.scenario.Spawn
import it.unibo.sentinel.core.robot.RobotId

object ScenarioEditor extends Editor:

  enum Command:
    /** Place a [[Spawn]] in the [[Scenario]].
      */
    case PlaceRobot(at: Position, ofClass: RobotClass)

  final case class State(scenario: Scenario, fail: Option[Validation] = None)

  type Model = Scenario

  override def model(state: State): Model = state.scenario

  import Command.*

  override def apply(state: State, command: Command): State = command match
    case PlaceRobot(at, ofClass) =>
      val rid = RobotId("r1")
      val spawn = Spawn(rid, at, ofClass)
      val withSpawn = state.scenario.place(spawn)
      withSpawn.fold(
        fail => State(state.scenario, Some(fail)),
        updated => State(updated, None)
      )
