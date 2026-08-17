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

enum MissionStatus:
  case Pending
  case Assigned

case class Mission(
  id: MissionID,
  carrier: Option[RobotID]
):
  def status: MissionStatus =
    carrier match
      case Some(_) => MissionStatus.Assigned
      case _ => MissionStatus.Pending
    
  
object Mission:
  def apply(
    id: MissionID
  ): Mission = new Mission(id, None)