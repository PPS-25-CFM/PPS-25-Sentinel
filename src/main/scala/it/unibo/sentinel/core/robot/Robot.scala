package it.unibo.sentinel.core.robot

import it.unibo.sentinel.core.mission.MissionId

/** Abstracts the concept of a robot, which is an entity capable of accepting
  * and executing missions while moving through the [[Warehouse]]
  */
trait Robot:

  /** @return
    *   the robot's identifier
    */
  def id: RobotId

  /** @return
    *   the id of the mission that the robot is currently executing
    */
  def mission: Option[MissionId]

  /** @return
    *   the robot's current operational status
    */
  def status: RobotStatus

  /** @return
    *   true if the robot can accept a new mission, false otherwise
    */
  def canAccept: Boolean

  /** Accepts a new mission (if possible)
    *
    * @param missionId
    *   the mission's id
    */
  def accept(missionId: MissionId): Unit

  /** Starts the mission
    */
  def startMission: Unit

  /** Interrupts and removes the mission
    */
  def dropMission: Unit

object Robot:
  /** @param id
    *   the robot's identifier
    * @return
    *   a new robot with the given id, no missions and idle status
    */
  def apply(id: RobotId): Robot = new SimpleRobot(id)

  /** Implementation of a [[Robot]] that can accept one mission
    */
  private class SimpleRobot(val id: RobotId) extends Robot:
    private var _mission: Option[MissionId] = None
    private var _status: RobotStatus = RobotStatus.Idle

    override def mission: Option[MissionId] = _mission

    override def status: RobotStatus = _status

    override def canAccept: Boolean = mission.isEmpty

    override def accept(missionId: MissionId): Unit =
      if canAccept then
        _mission = Some(missionId)
        _status = RobotStatus.Ready

    override def startMission: Unit =
      _status = RobotStatus.Moving

    override def dropMission: Unit =
      _mission = None
      _status = RobotStatus.Idle
