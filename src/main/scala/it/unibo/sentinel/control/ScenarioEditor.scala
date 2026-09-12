package it.unibo.sentinel.control

import it.unibo.sentinel.core.mission.{Mission, MissionId, Priority, Task}
import it.unibo.sentinel.core.robot.{RobotId, value}
import it.unibo.sentinel.core.scenario.{
  Policies,
  RobotClass,
  Scenario,
  Spawn,
  Validation
}
import it.unibo.sentinel.core.simulation.Tick
import it.unibo.sentinel.core.warehouse.{Position, Tile}

/** Interprets scenario edits without creating robots or running a simulation.
  */
object ScenarioEditor extends Editor:

  type Model = Scenario

  override def model(state: State): Model = state.scenario

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
      * Without a delivery whose destination has already been selected, this
      * command leaves the state unchanged.
      */
    case LoadDelivery(deadline: Tick, priority: Priority)

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

  /** The current selection in the [[ScenarioEditor]].
    */
  enum Selection:
    /** No selection is in progress.
      */
    case Empty

    /** Selects a single cell in the [[Scenario]].
      */
    case Cell(at: Position)

    /** Selects the delivery steps in the [[Scenario]].
      */
    case Delivery(from: Position, to: Option[Position])

    /** Returns the [[Position]] that is currently highlighted, if any.
      */
    def highlighted: Option[Position] = this match
      case Empty             => None
      case Cell(at)          => Some(at)
      case Delivery(from, _) => Some(from)

    /** Returns the origin [[Position]] of the delivery, if any.
      */
    def origin: Option[Position] = this match
      case Delivery(from, _) => Some(from)
      case _                 => None

  /** The state of an in-progress edit.
    *
    * @param scenario
    *   the [[Scenario]] as edited so far.
    * @param fail
    *   the [[Validation]] rejecting the last command, if any.
    * @param selection
    *   what the user is currently pointing at or composing.
    */
  final case class State(
      scenario: Scenario,
      fail: Option[Validation] = None,
      selection: Selection = Selection.Empty
  )

  import Command.*

  /** @param state
    *   the current editing state.
    * @param command
    *   the command issued by the user.
    * @return
    *   the [[State]] resulting from applying `command` to `state`.
    */
  override def apply(state: State, command: Command): State = command match
    case Select(at) =>
      state.selection match
        case Selection.Delivery(from, _) =>
          if state.scenario.warehouse.isLoadingBay(at) then
            state.copy(
              selection = Selection.Delivery(from, Some(at)),
              fail = None
            )
          else
            state.copy(
              selection = Selection.Delivery(from, None),
              fail = Some(Validation.NotLoadingBay(at))
            )
        case _ => state.copy(selection = Selection.Cell(at), fail = None)

    case BeginDelivery(from) =>
      if state.scenario.warehouse.isShelf(from) then
        state.copy(selection = Selection.Delivery(from, None), fail = None)
      else state.copy(fail = Some(Validation.NotShelfTile(from)))

    case CancelDelivery =>
      state.selection match
        case Selection.Delivery(from, _) =>
          state.copy(selection = Selection.Cell(from), fail = None)
        case _ => state.copy(fail = None)

    case LoadRelocation(to, deadline, priority) =>
      apply(state, LoadMission(Task.move(to), deadline, priority))

    case LoadDelivery(deadline, priority) =>
      state.selection match
        case Selection.Delivery(from, Some(to)) =>
          state.scenario.warehouse.tileAt(from) match
            case Some(Tile.Shelf(item)) =>
              val loaded = apply(
                state,
                LoadMission(
                  Task.pickAndDrop(item, from, to),
                  deadline,
                  priority
                )
              )
              if loaded.fail.isEmpty then
                loaded.copy(selection = Selection.Cell(to))
              else loaded
            case _ => state.copy(fail = Some(Validation.NotShelfTile(from)))
        case _ => state

    case PlaceRobot(at, ofClass) =>
      val id = freshRobotId(state.scenario)
      state.attempt:
        _.place(Spawn(id, at, ofClass))

    case LoadMission(task, deadline, priority) =>
      val id = freshMissionId(state.scenario)
      state.attempt:
        _.load(
          Mission(id, task, deadline, priority)
        )

    case RemoveRobot(id) =>
      state.edit(_.remove(id))

    case UnloadMission(id) =>
      state.edit(_.unload(id))

    case ChooseRouting(policy) =>
      state.edit(_.withRouting(policy))

    case ChooseAssigmnment(policy) =>
      state.edit(_.withAssignment(policy))

    case ChooseCollisionSelection(policy) =>
      state.edit(_.withCollisionSelection(policy))

    case ChooseCollisionAvoidance(policy) =>
      state.edit(_.withCollisionAvoidance(policy))

    case Reseed(seed) =>
      state.edit(_.withSeed(seed))

  extension (state: State)

    private def attempt(f: Scenario => Either[Validation, Scenario]): State =
      f(state.scenario).fold(
        fail => state.copy(fail = Some(fail)),
        updated => state.copy(scenario = updated, fail = None)
      )

    private def edit(f: Scenario => Scenario): State =
      state.copy(scenario = f(state.scenario), fail = None)

  private def freshRobotId(scenario: Scenario): RobotId =
    RobotId(nextId("R", scenario.spawns.map(_.id.value)))

  private def freshMissionId(scenario: Scenario): MissionId =
    MissionId(nextId("M", scenario.missions.map(_.id.value)))

  private def nextId(prefix: String, existingIds: Seq[String]): String =
    val next = existingIds
      .flatMap(_.stripPrefix(prefix).toIntOption)
      .maxOption
      .getOrElse(0) + 1
    s"$prefix$next"
