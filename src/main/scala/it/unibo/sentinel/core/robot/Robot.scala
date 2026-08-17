package it.unibo.sentinel.core.robot

type MissionId = String

/** */
trait Robot:

  /** @return the robot's identifier
    */
  def id: RobotId

  /** @return the id of the mission that the robot is currently executing
    */
  def mission: Option[MissionId]

object Robot:
  def apply(robotId: RobotId): Robot = new Robot:

    override def id: RobotId = robotId

    override def mission: Option[MissionId] = None
