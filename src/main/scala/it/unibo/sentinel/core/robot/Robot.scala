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

  /** Accepts a new mission
    * 
    * @param missionId the mission's id
    */
  def accept(missionId: MissionId): Unit

object Robot:
  def apply(id: RobotId): Robot = new SimpleRobot(id)
  
  private class SimpleRobot(val id: RobotId) extends Robot:
    private var _mission: Option[MissionId] = None

    override def mission: Option[MissionId] = _mission

    override def status: RobotStatus = RobotStatus.Idle

    override def canAccept: Boolean = mission.isEmpty

    override def accept(missionId: MissionId): Unit =
      _mission = Some(missionId)
