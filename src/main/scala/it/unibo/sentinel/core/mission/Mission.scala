package it.unibo.sentinel.core.mission

opaque type MissionID = String

object MissionID:
  /** */
  def apply(value: String): MissionID = value

  extension (id: MissionID)
    /** */
    def value: String = id

case class Mission(
  id: MissionID
)
  
object Mission:
  def apply(
    id: MissionID
  ): Mission = new Mission(id)