package it.unibo.sentinel.control

import it.unibo.sentinel.core.scenario.Scenario
import it.unibo.sentinel.core.warehouse.{Position, Tile}
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

/** Interprets scenario edits without creating robots or running a simulation.
  */
object ScenarioEditor extends Editor:

  enum Command:
    /** Select a [[Position]] in the editor. */
    case Select(at: Position)

    /** Remember a shelf while the user chooses a delivery [[Position]]. */
    case BeginDelivery(from: Position)

    /** Discard the delivery under construction. */
    case CancelDelivery

    /** Load a movement mission to a traversable cell. */
    case LoadRelocation(to: Position, deadline: Tick, priority: Priority)

    /** Complete the pending delivery using the item stored on its shelf.
      * Without an origin, this command leaves the state unchanged.
      */
    case LoadDelivery(to: Position, deadline: Tick, priority: Priority)

    /** Place a [[Spawn]] in the [[Scenario]].
      */
    case PlaceRobot(at: Position, ofClass: RobotClass)

    /** Load a [[Mission]] in the [[Scenario]].
      */
    case LoadMission(task: Task, deadline: Tick, priority: Priority)

    /** Remove a [[Spawn]] from the [[Scenario]].
      */
    case RemoveRobot(id: RobotId)

    /** Unload a [[Mission]] from the [[Scenario]].
      */
    case UnloadMission(id: MissionId)

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

  /** A pending delivery contains only its origin; no partial mission is saved.
    */
  final case class State(
      scenario: Scenario,
      fail: Option[Validation] = None,
      selection: Option[Position] = None,
      deliveryOrigin: Option[Position] = None
  )

  type Model = Scenario

  override def model(state: State): Model = state.scenario

  import Command.*

  override def apply(state: State, command: Command): State = command match
    case Select(at) =>
      state.copy(
        selection = Some(at),
        fail = Option.when(
          state.deliveryOrigin.isDefined && !state.scenario.warehouse
            .isLoadingBay(at)
        )(
          Validation.NotLoadingBay(at)
        )
      )
    case BeginDelivery(from) =>
      if state.scenario.warehouse.isShelf(from) then
        state.copy(
          selection = Some(from),
          deliveryOrigin = Some(from),
          fail = None
        )
      else state.copy(fail = Some(Validation.NotShelfTile(from)))
    case CancelDelivery => state.copy(deliveryOrigin = None, fail = None)
    case LoadRelocation(to, deadline, priority) =>
      apply(state, LoadMission(Task.move(to), deadline, priority))
    case LoadDelivery(to, deadline, priority) =>
      state.deliveryOrigin.fold(state): from =>
        state.scenario.warehouse.tileAt(from) match
          case Some(Tile.Shelf(item)) =>
            val loaded = apply(
              state,
              LoadMission(Task.pickAndDrop(item, from, to), deadline, priority)
            )
            if loaded.fail.isEmpty then loaded.copy(deliveryOrigin = None)
            else loaded
          case _ => state.copy(fail = Some(Validation.NotShelfTile(from)))
    case PlaceRobot(at, ofClass) =>
      val rid = state.scenario.freshRobotId
      state.attempt:
        _.place(Spawn(rid, at, ofClass))
    case LoadMission(task, deadline, priority) =>
      val mid = state.scenario.freshMissionId
      state.attempt:
        _.load(Mission(mid, task, deadline, priority))
    case RemoveRobot(id) =>
      state.edit:
        _.remove(id)
    case UnloadMission(id) =>
      state.edit:
        _.unload(id)
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
        fail => state.copy(fail = Some(fail)),
        updated => state.copy(scenario = updated, fail = None)
      )

    private def edit(f: Scenario => Scenario): State =
      state.copy(scenario = f(state.scenario), fail = None)

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
