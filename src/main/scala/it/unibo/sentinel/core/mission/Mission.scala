package it.unibo.sentinel.core.mission

import it.unibo.sentinel.core.warehouse.Position
import it.unibo.sentinel.core.robot.RobotId

opaque type MissionId = String

object MissionId:
  def apply(value: String): MissionId = value

  extension (id: MissionId) def value: String = id

// TODO: Update when the concept of Tick will be introduced
type Ticks = Int

enum MissionStatus:
  case Pending
  case Assigned
  case Completed
  case Failed

enum Task:
  case Done
  case Fail
  case Move(at: Position)

  def isDone: Boolean = this == Task.Done
  def isFail: Boolean = this == Task.Fail

  def where: Option[Position] = this match
    case Move(at) => Some(at)
    case _ => None

case class Mission private (
    id: MissionId,
    task: Task,
    duration: Ticks,
    carrier: Option[RobotId]
):
  import MissionStatus.*

  def isPending: Boolean = status == Pending

  def isOver: Boolean = status match
    case Completed | Failed => true
    case _                  => false

  def status: MissionStatus =
    if task.isFail || duration <= 0 then Failed
    else if task.isDone then Completed
    else if carrier.isDefined then Assigned
    else Pending

  def assignTo(robotID: RobotId): Mission =
    if isPending then copy(carrier = Some(robotID)) else this

  def unassign: Mission =
    if isOver then this else copy(carrier = None)

  def complete: Mission =
    if isOver then this else copy(task = Task.Done)

  def fail: Mission =
    if isOver then this else copy(task = Task.Fail)

  def proceed: Mission =
    if isOver then this
    else if duration <= 1 then copy(duration = 0).fail
    else copy(duration = duration - 1)

object Mission:
  def apply(
      id: MissionId,
      task: Task,
      duration: Ticks
  ): Mission = new Mission(id, task, duration, None)
