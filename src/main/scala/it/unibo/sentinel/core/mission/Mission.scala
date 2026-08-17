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

enum MissionStatus:
  case Pending
  case Assigned

case class Mission private(
  id: MissionID,
  destination: Position,
  carrier: Option[RobotID]
):
  def isPending: Boolean = status == MissionStatus.Pending

  def status: MissionStatus =
    carrier match
      case Some(_) => MissionStatus.Assigned
      case _ => MissionStatus.Pending

  def assignTo(robotID: RobotID): Mission =
    if isPending then copy(carrier = Some(robotID)) else this
    
object Mission:
  def apply(
    id: MissionID,
    position: Position
  ): Mission = new Mission(id, position, None)