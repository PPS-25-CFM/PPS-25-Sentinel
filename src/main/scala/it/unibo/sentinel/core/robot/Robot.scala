package it.unibo.sentinel.core.robot

import it.unibo.sentinel.core.mission.MissionId
import it.unibo.sentinel.core.routing.Path
import it.unibo.sentinel.core.warehouse.Position
import it.unibo.sentinel.core.simulation.Tick
import scala.collection.immutable.Queue
import it.unibo.sentinel.core.item.Weight
import it.unibo.sentinel.core.item.Item
import it.unibo.sentinel.core.mission.Mission

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
    *   the number of missions currently assigned to the robot.
    */
  def workload: Workload = Workload(mission.size)

  /** @return
    *   the robot's current operational status
    */
  def status: RobotStatus

  /** @return
    *   true if the robot can accept a new mission, false otherwise
    */
  def canAccept(mission: Mission): Boolean

  /** Accepts a new mission (if possible)
    *
    * @param missionId
    *   the mission's id
    */
  def accept(mission: Mission): Unit

  /** Interrupts and removes the mission
    */
  def release(): Unit

  /** Clears the current path without removing the mission.
    */
  def clearRoute(): Unit

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

  /** Pauses the robot's movement
    */
  def pause(): Unit

  /** Resumes the robot's movement
    */
  def resume(): Unit

  /** Advances the robot's internal clock by one tick.
    */
  def tick(): Unit

  /** Picks up an [[Item]] into the robot's bag (if possible)
    *
    * @param item
    *   the [[Item]] to pick up
    * @return
    *   true if the item was picked up, false otherwise
    */
  def pick(item: Item): Boolean

  /** Drops an [[Item]] from the robot's bag
    *
    * @param item
    *   the [[Item]] to drop
    * @return
    *   [[Some]] with the dropped [[Item]] if it was carried, [[None]] otherwise
    */
  def drop(item: Item): Option[Item]

/** [[Robot]] capable of accepting multiple [[Mission]]s using a queue.
  *
  * @param capacity
  *   max number of [[Mission]]s that the [[Robot]] can accept.
  */
trait Queued(capacity: Int) extends Robot:

  private var backlog: Queue[MissionId] = Queue.empty

  /** @return
    *   whether there is queue capacity and the underlying robot can accept
    *   `mission`.
    */
  abstract override def canAccept(mission: Mission): Boolean =
    backlog.size < capacity && super.canAccept(mission)

  /** Enqueues `mission` if [[canAccept]] holds. */
  override def accept(mission: Mission): Unit =
    if canAccept(mission) then backlog = backlog :+ mission.id

  /** @return the head of the mission queue, if any. */
  override def mission: Option[MissionId] = backlog.headOption

  /** @return
    *   the number of missions in queue, including the one under execution.
    */
  override def workload: Workload = Workload(backlog.size)

  /** Dequeues the current mission and its queue to the underlying robot. */
  abstract override def release(): Unit =
    backlog = backlog match
      case _ +: tail => tail
      case _         => Queue.empty
    super.release()

object Robot:
  /** @param id
    *   the robot's identifier
    * @param capacity
    *   max number of missions the robot can queue
    * @return
    *   a new drone with the given id, no missions and idle status
    */
  def drone(id: RobotId, capacity: Int = 1): Robot = new Drone(id)
    with Queued(capacity)

  def lightCarrier(id: RobotId, capacity: Int = 1): Robot =
    new Carrier(id, Weight.average) with Queued(capacity)

  def heavyCarrier(id: RobotId, capacity: Int = 1): Robot =
    new Carrier(id, Weight.max) with Queued(capacity)

  /** Shared movement logic for all mobile robots.
    */
  private abstract class BaseRobot(val id: RobotId) extends Robot:

    private var waiting: Boolean = false
    private var currentPath: Option[Path] = None

    override def status: RobotStatus =
      if waiting then RobotStatus.Waiting
      else
        (mission, currentPath) match
          case (None, None)    => RobotStatus.Idle
          case (Some(_), None) => RobotStatus.Ready
          case (_, Some(path)) =>
            if path.positions.isEmpty then RobotStatus.Ready
            else RobotStatus.Moving

    override def release(): Unit =
      waiting = false
      currentPath = None

    override def clearRoute(): Unit =
      currentPath = None

    override def path: Option[Path] = currentPath

    override def follow(path: Path): Unit = currentPath = Some(path)

    override def next: Option[Position] =
      currentPath.flatMap(_.positions.headOption)

    override def pause(): Unit =
      if status == RobotStatus.Moving then waiting = true

    override def resume(): Unit = waiting = false

    override def step(): Unit =
      currentPath = currentPath.map(_.advanced)

    override def remaining: Tick =
      currentPath.map(_.remaining).getOrElse(Tick.zero)

    override def tick(): Unit =
      currentPath = currentPath.map(_.ticked)

  /** [[Robot]] accepting only relocation missions.
    */
  private abstract class Drone(id: RobotId) extends BaseRobot(id):
    override def canAccept(mission: Mission): Boolean =
      mission.isMovementOnly

    override def pick(item: Item): Boolean = false

    override def drop(item: Item): Option[Item] = None

  /** [[Robot]] accepting both relocation and delivery missions.
    *
    * @param maxLoad
    *   max transportable [[ItemWeight]]
    */
  private abstract class Carrier(id: RobotId, maxLoad: Weight)
      extends BaseRobot(id):
    private var bag: Seq[Item] = Seq.empty

    private def currentLoad: Weight =
      bag.map(_.weight).foldLeft(Weight.zero)(_ + _)

    override def canAccept(mission: Mission): Boolean =
      mission.isMovementOnly ||
        (mission.requiresCarrying && currentLoad.value <= maxLoad.value)

    override def pick(item: Item): Boolean =
      if (currentLoad + item.weight).value <= maxLoad.value then
        bag = bag :+ item
        true
      else false

    override def drop(item: Item): Option[Item] =
      bag
        .find(_ == item)
        .map: found =>
          bag = bag.diff(Seq(found))
          found
