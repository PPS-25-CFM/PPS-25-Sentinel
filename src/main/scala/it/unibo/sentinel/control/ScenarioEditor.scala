package it.unibo.sentinel.control

import it.unibo.sentinel.core.scenario.Scenario
import it.unibo.sentinel.core.warehouse.Position
import it.unibo.sentinel.core.scenario.RobotClass
import it.unibo.sentinel.core.scenario.Validation
import it.unibo.sentinel.core.scenario.Spawn
import it.unibo.sentinel.core.robot.RobotId
import it.unibo.sentinel.core.robot.value
import it.unibo.sentinel.core.mission.Task
import it.unibo.sentinel.core.simulation.Tick
import it.unibo.sentinel.core.mission.Priority
import it.unibo.sentinel.core.mission.Mission
import it.unibo.sentinel.core.mission.MissionId
import it.unibo.sentinel.core.scenario.Policies

object ScenarioEditor extends Editor:

  enum Command:
    /** Place a [[Spawn]] in the [[Scenario]].
      */
    case PlaceRobot(at: Position, ofClass: RobotClass)

    /** Load a [[Mission]] in the [[Scenario]].
      */
    case LoadMission(task: Task, deadline: Tick, priority: Priority)

    /** Choose a routing policy for the [[Scenario]].
      */
    case ChooseRouting(policy: Policies.Routing)

    /** Choose an assignment policy for the [[Scenario]].
      */
    case ChooseAssigmnment(policy: Policies.Assignment)

    /** Choose a collision selection policy for the [[Scenario]].
      */
    case ChooseCollisionSelection(policy: Policies.CollisionSelection)

    /** Choose a collision avoidance policy for the [[Scenario]].
      */
    case ChooseCollisionAvoidance(policy: Policies.CollisionAvoidance)

    /** Reseed the random generator of the [[Scenario]].
      */
    case Reseed(seed: Long)

  final case class State(scenario: Scenario, fail: Option[Validation] = None)

  type Model = Scenario

  override def model(state: State): Model = state.scenario

  import Command.*

  override def apply(state: State, command: Command): State = command match
    case PlaceRobot(at, ofClass) =>
      val rid = state.scenario.freshRobotId
      state.attempt:
        _.place(Spawn(rid, at, ofClass))
    case LoadMission(task, deadline, priority) =>
      val mid = state.scenario.freshMissionId
      state.attempt:
        _.load(Mission(mid, task, deadline, priority))
    case ChooseRouting(policy) =>
      state.edit:
        _.withRouting(policy)
    case ChooseAssigmnment(policy) =>
      state.edit:
        _.withAssignment(policy)
    case ChooseCollisionSelection(policy) =>
      state.edit:
        _.withCollisionSelection(policy)
    case ChooseCollisionAvoidance(policy) =>
      state.edit:
        _.withCollisionAvoidance(policy)
    case Reseed(seed) =>
      state.edit:
        _.withSeed(seed)

  extension (state: State)

    private def attempt(f: Scenario => Either[Validation, Scenario]): State =
      f(state.scenario).fold(
        fail => State(state.scenario, Some(fail)),
        updated => State(updated, None)
      )

    private def edit(f: Scenario => Scenario): State =
      State(f(state.scenario))

  extension (sc: Scenario)
    private def freshRobotId: RobotId =
      val existingIds = sc.spawns.map(_.id.value)
      RobotId(nextId("R", existingIds))

    private def freshMissionId: MissionId =
      val existingIds = sc.missions.map(_.id.value)
      MissionId(nextId("M", existingIds))

    private def nextId(prefix: String, existingIds: Seq[String]): String =
      val nextId = existingIds
        .map(_.stripPrefix(prefix).toIntOption.getOrElse(0))
        .maxOption
        .getOrElse(0) + 1
      s"$prefix$nextId"
