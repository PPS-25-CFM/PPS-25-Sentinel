package it.unibo.sentinel.core.robot

import it.unibo.sentinel.core.mission.MissionId
import it.unibo.sentinel.core.routing.Path
import it.unibo.sentinel.core.warehouse.Position
import it.unibo.sentinel.core.simulation.Tick

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

  /** Interrupts and removes the mission
    */
  def release(): Unit

  /** Sets a [[Path]] to follow
    *
    * @param path
    *   a sequence of [[Position]]s
    */
  def follow(path: Path): Unit

  /** @return
    *   the [[Path]] that the robot is currently following
    */
  def path: Option[Path]

  /** @return
    *   the next [[Position]] in the robot's [[Path]] (if there is one)
    */
  def next: Option[Position]

  /** @return
    *   the remaining time before the robot can move to the next [[Position]] in
    *   its [[Path]] (if there is one).
    */
  def remaining: Tick

  /** Advances its [[Path]]
    */
  def step(): Unit

  /** Advances the robot's internal clock by one tick.
    */
  def tick(): Unit

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

    private var currentMission: Option[MissionId] = None
    private var currentPath: Option[Path] = None

    override def mission: Option[MissionId] = currentMission

    override def status: RobotStatus = (currentMission, currentPath) match
      case (None, None)    => RobotStatus.Idle
      case (Some(_), None) => RobotStatus.Ready
      case (_, Some(_))    => RobotStatus.Moving

    override def canAccept: Boolean = mission.isEmpty

    override def accept(missionId: MissionId): Unit =
      if canAccept then currentMission = Some(missionId)

    override def release(): Unit =
      currentMission = None
      currentPath = None

    override def path: Option[Path] = currentPath

    override def follow(path: Path): Unit =
      currentPath = Some(path)

    override def next: Option[Position] =
      currentPath.flatMap(_.positions.headOption)

    override def step(): Unit =
      currentPath = currentPath.map(_.advanced)

    override def remaining: Tick =
      currentPath.map(_.remaining).getOrElse(Tick.zero)

    override def tick(): Unit =
      currentPath = currentPath.map(_.ticked)
