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

case class Mission(
  id: MissionID,
  carrier: Option[RobotID]
)
    
  
object Mission:
  def apply(
    id: MissionID
  ): Mission = new Mission(id, None)