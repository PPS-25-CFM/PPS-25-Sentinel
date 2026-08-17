package it.unibo.sentinel.core.robot

/** Unique identifier for [[Robot]]s
  */
opaque type RobotId = String

object RobotId:
  def apply(id: String): RobotId = id

extension (id: RobotId)
  /** @return
    *   the identifier as a String
    */
  def value: String = id
