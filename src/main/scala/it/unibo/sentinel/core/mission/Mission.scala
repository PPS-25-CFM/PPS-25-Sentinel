package it.unibo.sentinel.core.mission

import it.unibo.sentinel.core.robot.RobotId
import it.unibo.sentinel.core.warehouse.Position

// TODO: Update when the concept of Tick will be introduced
type Ticks = Int

/** Domain context entity representing a mission within the Sentinel system.
  *
  * @param id
  *   Unique identifier of the mission.
  * @param task
  *   The operational task to be performed within this mission.
  * @param duration
  *   Remaining mission execution window measured in [[Ticks]].
  * @param status
  *   The current lifecycle state of the mission.
  * @param carrier
  *   The robot currently assigned to carry out the mission, if any.
  */
final case class Mission private (
    id: MissionId,
    task: Task,
    duration: Ticks,
    status: MissionStatus,
    carrier: Option[RobotId]
):
  import MissionStatus.*

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

  /** Retrieves the target position of the current step.
    *
    * @return
    *   [[Some]] destination [[Position]] if the mission is active, or [[None]]
    *   if it has reached a terminal state.
    */
  def currentDestination: Option[Position] =
    if isOver then None
    else Some(task.destination)

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
    if isOver then this
    else copy(carrier = None, status = Pending)

  /** @return
    *   A new completed [[Mission]], or the unchanged [[Mission]] if it is
    *   already over.
    */
  def complete: Mission =
    if isOver then this else unassign.copy(status = Completed)

  /** @return
    *   A new failed [[Mission]], or the unchanged [[Mission]] if it is already
    *   over.
    */
  def fail: Mission =
    if isOver then this else unassign.copy(status = Failed)

  /** @return
    *   A new [[Mission]] with updated duration, or failed if duration expires.
    *   Returns the unchanged [[Mission]] if it is already over.
    */
  def proceed: Mission =
    if isOver then this
    else if duration <= 1 then copy(duration = 0).fail
    else copy(duration = duration - 1)

object Mission:
  /** Factory method to instantiate a new mission.
    *
    * @param id
    *   The unique identifier for the mission.
    * @param task
    *   The task to be completed.
    * @param duration
    *   The total time window allocated for the mission, expressed in [[Ticks]].
    * @return
    *   A new [[Mission]] initialized in the unassigned
    *   [[MissionStatus.Pending]] state.
    */
  def apply(
      id: MissionId,
      task: Task,
      duration: Ticks
  ): Mission = new Mission(
    id,
    task,
    duration,
    MissionStatus.Pending,
    None
  )
