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

  /** @return
    *   the robot's current operational status
    */
  def status: RobotStatus

  /** @return true if the robot can accept a new mission, false otherwise
    */
  def canAccept: Boolean

object Robot:
  def apply(robotId: RobotId): Robot = new Robot:

    override def id: RobotId = robotId

    override def mission: Option[MissionId] = None

    override def status: RobotStatus = RobotStatus.Idle

    override def canAccept: Boolean = true
