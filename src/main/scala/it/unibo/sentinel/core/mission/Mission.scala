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
case class Mission private (
    id: MissionId,
    task: Task,
    duration: Ticks,
    carrier: Option[RobotId]
):
  import MissionStatus.*

  /** Returns true if the mission is currently waiting to be assigned to a
    * robot.
    */
  def isPending: Boolean = status == Pending

  /** Returns true if the mission has reached a terminal state (Completed or
    * Failed).
    */
  def isOver: Boolean = status match
    case Completed | Failed => true
    case _                  => false

  /** Dynamically computes the current lifecycle status of the mission. */
  def status: MissionStatus =
    if task.isFail || duration <= 0 then Failed
    else if task.isDone then Completed
    else if carrier.isDefined then Assigned
    else Pending

  /** Assigns the mission to the specified robot.
    */
  def assignTo(robotID: RobotId): Mission =
    if isPending then copy(carrier = Some(robotID)) else this

  /** Unassigns the currently assigned robot.
    */
  def unassign: Mission =
    if isOver then this else copy(carrier = None)

  /** Marks the task and the mission as completed.
    */
  def complete: Mission =
    if isOver then this else copy(task = Task.Done)

  /** Marks the task and the mission as failed.
    */
  def fail: Mission =
    if isOver then this else copy(task = Task.Fail)

  /** Advances the mission lifecycle by one tick. */
  def proceed: Mission =
    if isOver then this
    else if duration <= 1 then copy(duration = 0).fail
    else copy(duration = duration - 1)

object Mission:
  /** Factory method to create a new Mission in its initial unassigned (Pending)
    * state.
    */
  def apply(
      id: MissionId,
      task: Task,
      duration: Ticks
  ): Mission = new Mission(id, task, duration, None)
