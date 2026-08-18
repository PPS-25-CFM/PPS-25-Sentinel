package it.unibo.sentinel.core.mission

import it.unibo.sentinel.core.robot.RobotId

// TODO: Update when the concept of Tick will be introduced
type Ticks = Int

/** Domain context entity representing a mission within the Sentinel system.
  *
  * @param id
  *   Unique identifier of the mission.
  * @param task
  *   The task to be performed within this mission.
  * @param duration
  *   Remaining mission duration measured in Ticks.
  * @param carrier
  *   The robot currently assigned to carry out the mission, if any.
  */
final case class Mission private (
    id: MissionId,
    task: Task,
    duration: Ticks,
    carrier: Option[RobotId]
):
  import MissionStatus.*

  /** @return
    *   whether the [[Mission]] is currently waiting to be assigned to a
    *   [[Robot]].
    */
  def isPending: Boolean = status == Pending

  /** @return
    *   whether the [[Mission]] has reached a terminal state ([[Completed]] or
    *   [[Failed]]).
    */
  def isOver: Boolean = status match
    case Completed | Failed => true
    case _                  => false

  /** Dynamically computes the current lifecycle status of the [[Mission]]. */
  def status: MissionStatus = (task, carrier) match
    case (task, _) if task.isFail          => Failed
    case (task, _) if task.isDone          => Completed
    case (_, carrier) if carrier.isDefined => Assigned
    case _                                 => Pending

  /** @param robotID
    *   Identifier of the mission carrier
    * @return
    *   A new [[Mission]] with the assigned [[Robot]].
    */
  def assignTo(robotID: RobotId): Mission =
    if isPending then copy(carrier = Some(robotID)) else this

  /** @return
    *   A new [[Mission]] without an assigned [[Robot]].
    */
  def unassign: Mission =
    if isOver then this else copy(carrier = None)

  /** @return
    *   A new completed [[Mission]].
    */
  def complete: Mission =
    if isOver then this else copy(task = Task.Done)

  /** @return
    *   A new failed [[Mission]].
    */
  def fail: Mission =
    if isOver then this else copy(task = Task.Fail)

  /** @return
    *   A new completed [[Mission]] with its lifecycle advanced by one [[Tick]].
    */
  def proceed: Mission =
    if isOver then this
    else if duration <= 1 then copy(duration = 0).fail
    else copy(duration = duration - 1)

object Mission:
  /** @param id
    *   The mission unique ID.
    * @param task
    *   The task to complete.
    * @param duration
    *   The expiration time of the mission expressed by [[Ticks]] remaining
    * @return
    *   A new Mission in its initial unassigned (Pending) state.
    */
  def apply(
      id: MissionId,
      task: Task,
      duration: Ticks
  ): Mission = new Mission(id, task, duration, None)
