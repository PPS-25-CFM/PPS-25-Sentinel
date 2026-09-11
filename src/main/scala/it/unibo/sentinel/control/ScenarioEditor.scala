package it.unibo.sentinel.control

import it.unibo.sentinel.core.scenario.Scenario
import it.unibo.sentinel.core.warehouse.Position
import it.unibo.sentinel.core.scenario.RobotClass
import it.unibo.sentinel.core.scenario.Validation
import it.unibo.sentinel.core.scenario.Spawn
import it.unibo.sentinel.core.robot.RobotId
import it.unibo.sentinel.core.robot.value

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
      val current = state.scenario
      val rid = current.freshRobotId
      val spawn = Spawn(rid, at, ofClass)
      current
        .place(spawn)
        .fold(
          fail => State(current, Some(fail)),
          updated => State(updated, None)
        )

  extension (sc: Scenario)
    private def freshRobotId: RobotId =
      val existingIds = sc.spawns.map(_.id)
      val nextId = existingIds
        .map(_.value.stripPrefix("R").toIntOption.getOrElse(0))
        .maxOption
        .getOrElse(0) + 1
      RobotId(s"R$nextId")
