package it.unibo.sentinel.core.mission

import it.unibo.sentinel.core.robot.RobotId
import it.unibo.sentinel.core.warehouse.Position
import it.unibo.sentinel.core.simulation.Tick

/** Domain context entity representing a mission within the Sentinel system.
  *
  * @param id
  *   Unique identifier of the mission.
  * @param task
  *   The operational task to be performed within this mission.
  * @param deadline
  *   Remaining mission execution window measured in [[Tick]].
  * @param status
  *   The current lifecycle state of the mission.
  * @param carrier
  *   The robot currently assigned to carry out the mission, if any.
  */
final case class Mission private (
    id: MissionId,
    task: Task,
    deadline: Tick,
    status: MissionStatus,
    carrier: Option[RobotId]
):
  import MissionStatus.*

  private def unlessOver(f: => Mission): Mission =
    if isOver then this else f

  private def whenAssigned(f: => Mission): Mission =
    if status == Assigned then f else this

  private def terminateAs(s: MissionStatus): Mission =
    unassign.copy(status = s)

  /** Retrieves the current action of the mission's task.
    *
    * @return
    *   An [[Action]] if the mission is active, or [[None]] if it has reached a
    *   terminal state.
    */
  def currentAction: Option[Action] =
    if isOver then None else task.currentAction

  /** @return
    *   whether the [[Mission]] is currently waiting to be assigned to a
    *   carrier.
    */
  def isPending: Boolean = status == Pending

  /** @return
    *   whether the [[Mission]] has reached a terminal state ([[Completed]] or
    *   [[Failed]]).
    */
  def isOver: Boolean = status match
    case Completed | Failed => true
    case _                  => false

  /** Retrieves the target position of the current action.
    *
    * @return
    *   A [[Position]] if the mission is active, or [[None]] if it has reached a
    *   terminal state.
    */
  def currentTarget: Option[Position] =
    currentAction.map(_.position)

  /** @param robotID
    *   Identifier of the robot assigned to carry out the mission.
    * @return
    *   A new [[Mission]] assigned to the specified robot with status set to
    *   [[Assigned]], or the unchanged [[Mission]] if it was not in [[Pending]]
    *   status.
    */
  def assignTo(robotID: RobotId): Mission =
    if isPending then copy(carrier = Some(robotID), status = Assigned)
    else this

  /** @return
    *   A new [[Mission]] without an assigned carrier and status reset to
    *   [[Pending]], or the unchanged [[Mission]] if it has already reached a
    *   terminal state.
    */
  def unassign: Mission =
    unlessOver(copy(carrier = None, status = Pending))

  /** @return
    *   A new completed [[Mission]], or the unchanged [[Mission]] if it is not
    *   [[Assigned]] or already over.
    */
  def complete: Mission =
    whenAssigned(terminateAs(Completed))

  /** @return
    *   A new failed [[Mission]], or the unchanged [[Mission]] if it is already
    *   over.
    */
  def fail: Mission =
    unlessOver(terminateAs(Failed))

  /** @return
    *   A new [[Mission]] with the updated task execution state, or the
    *   unchanged [[Mission]] if it is not [[Assigned]] or already over.
    */
  def completeCurrentAction: Mission =
    whenAssigned:
      task.advance match
        case Task.Done => copy(task = Task.Done).complete
        case next      => copy(task = next)

  /** @return
    *   A new [[Mission]] with updated duration, or failed if duration expires.
    *   Returns the unchanged [[Mission]] if it is already over.
    */
  def tick: Mission =
    unlessOver:
      if deadline.value <= 1 then copy(deadline = deadline.previous).fail
      else copy(deadline = deadline.previous)

object Mission:

  /** @param id
    *   The unique identifier for the mission.
    * @param task
    *   The task to be completed.
    * @param duration
    *   The total time window allocated for the mission, expressed in [[Tick]]
    *   units.
    * @return
    *   A new [[Mission]] initialized in the unassigned
    *   [[MissionStatus.Pending]] state.
    */
  private def apply(
      id: MissionId,
      task: Task,
      deadline: Tick
  ): Mission = new Mission(
    id,
    task,
    deadline,
    MissionStatus.Pending,
    None
  )

  /** @param id
    *   The unique identifier for the mission.
    * @param destination
    *   The target [[Position]] to relocate towards.
    * @param duration
    *   The total time window allocated for the relocation, expressed in
    *   [[Tick]] units.
    * @return
    *   A new relocation [[Mission]] initialized in the unassigned
    *   [[MissionStatus.Pending]] state.
    */
  def relocate(id: MissionId, destination: Position, duration: Tick): Mission =
    Mission(id, Task.move(destination), duration)
