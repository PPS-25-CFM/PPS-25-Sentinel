package it.unibo.sentinel.core.mission

opaque type MissionID = String

object MissionID:
  /** */
  def apply(value: String): MissionID = value

  extension (id: MissionID)
    /** */
    def value: String = id

// TODO: Update when Robot will be introduced
type RobotID = String

// TODO: Update when the concept of Position will be introduced
type Position = (Int, Int)

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
  case MoveTo(at: Position)

  def isDone: Boolean = this match
    case Done => true
    case _    => false

  def isFail: Boolean = this match
    case Fail => true
    case _    => false

case class Mission private(
  id: MissionID,
  task: Task,
  duration: Ticks,
  carrier: Option[RobotID]
):
  import MissionStatus.*

  def isPending: Boolean = status == Pending

  def isOver: Boolean = status match
    case Completed | Failed => true
    case _ => false

  def status: MissionStatus =
    if task.isFail || duration <= 0 then Failed
      else if task.isDone then Completed
      else
        carrier match
          case None    => Pending
          case Some(_) => Assigned

  def assignTo(robotID: RobotID): Mission =
    if isPending then copy(carrier = Some(robotID)) else this

  def unassign: Mission =
    copy(carrier = None) 

  def complete: Mission =
    if isOver then this else copy(task = Task.Done)

  def fail: Mission =
    if isOver then this else copy(task = Task.Fail)

  def proceed: Mission =
    if isOver then this else if duration - 1 <= 0 then copy(duration = 0).fail else copy(duration = duration - 1)

object Mission:
  def apply(
    id: MissionID,
    task: Task,
    duration: Ticks
  ): Mission = new Mission(id, task, duration, None)