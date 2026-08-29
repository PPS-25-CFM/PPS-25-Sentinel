package it.unibo.sentinel.core.simulation

import it.unibo.sentinel.core.robot.{Robot, RobotId}
import it.unibo.sentinel.core.scenario.Placement
import it.unibo.sentinel.core.warehouse.Warehouse
import it.unibo.sentinel.core.mission.{Mission, MissionId, MissionStatus}
import it.unibo.sentinel.core.routing.Path
import it.unibo.sentinel.core.robot.RobotStatus

/** Provides query operations to inspect the state of the simulation.
  */
trait Queries:
  import it.unibo.sentinel.core.robot.{Robot, RobotStatus}

  /** @return
    *   the current [[Placement]]s of all robots in the simulation.
    */
  def placements: Seq[Placement]

  /** @return
    *   all [[Mission]]s present in the simulation.
    */
  def missions: Seq[Mission]

  /** @param robotId
    *   the [[RobotId]] of the robot to query.
    * @return
    *   the [[Robot]] with the given id, if any.
    */
  def robot(r_id: RobotId): Option[Robot]

  /** @param m_id
    *   the [[MissionId]] of the mission to query
    * @return
    *   the [[Mission]] with the given id, if any.
    */
  def mission(m_id: MissionId): Option[Mission]

  /** @param robotId
    *   the [[RobotId]] of the robot contained in the placement to query.
    * @return
    *   the [[Placement]] with the given id, if any.
    */
  def placement(r_id: RobotId): Option[Placement]

  /** @param status
    *   the [[RobotStatus]] to filter robots by.
    * @return
    *   the [[Placement]]s of robots that currently have the given
    *   [[RobotStatus]].
    */
  def standing(status: RobotStatus): Seq[Placement] =
    placements.filter(_.robot.status == status)

  /** @return
    *   all [[Mission]]s currently in [[Pending]] status.
    */
  def pendingMissions: Seq[Mission] =
    missions.filter(_.status == MissionStatus.Pending)

/** Represents the mutable simulation state, maintaining the [[Warehouse]]
  * layout, the robot [[fleet]], and the mission [[board]].
  *
  * @param warehouse
  *   the [[Warehouse]] where the simulation takes place.
  * @param fleet
  *   the map of current [[Placement]]s indexed by [[RobotId]].
  * @param board
  *   the map of active [[Mission]]s indexed by [[MissionId]].
  */
private[core] final class Environment private[core] (
    val warehouse: Warehouse,
    private var fleet: Map[RobotId, Placement],
    private var board: Map[MissionId, Mission]
) extends Queries:

  override def placements: Seq[Placement] = fleet.values.toSeq
  override def missions: Seq[Mission] = board.values.toSeq

  override def robot(r_id: RobotId): Option[Robot] =
    fleet.get(r_id).map(_.robot)

  override def mission(m_id: MissionId): Option[Mission] =
    board.get(m_id)

  override def placement(r_id: RobotId): Option[Placement] =
    fleet.get(r_id)

  /** @param r_id
    * @param m_id
    * @return
    *   an [[Event]] if the assignment was successful, None otherwise
    */
  def assign(r_id: RobotId, m_id: MissionId): Option[Event] =
    for
      spot <- fleet.get(r_id)
      robot = spot.robot
      mission <- board.get(m_id)
    yield
      robot.accept(m_id)
      board = board + (m_id -> mission.assignTo(r_id))
      Event.MissionAssigned(r_id, m_id)

  /** @param r_id
    * @param path
    * @return
    *   an [[Event]] if the routing was successful, None otherwise
    */
  def route(r_id: RobotId, path: Path): Option[Event] =
    for
      spot <- fleet.get(r_id)
      robot = spot.robot
    yield
      robot.follow(path)
      Event.RobotRouted(r_id, path.positions)

  /** @param r_id
    * @return
    *   an [[Event]] if the [[Robot]] was able to move, None otherwise
    */
  def advance(r_id: RobotId): Option[Event] =
    for
      spot <- fleet.get(r_id)
      robot = spot.robot
      from = spot.at
      intent = spot.intent
      if robot.remaining == Tick.zero
    yield
      if canMove(spot) then
        robot.step()
        fleet += (r_id -> spot.copy(at = intent.position))
        Event.RobotMoved(r_id, from, intent.position)
      else
        spot.robot.pause()
        Event.RobotBlocked(r_id, from)

  private def canMove(placement: Placement): Boolean =
    placement.robot.status == RobotStatus.Moving
      && placement.robot.remaining == Tick.zero
      && fleet.values.forall { other =>
        val targetPositionOccupied = other.at == placement.intent.position
        lazy val targetWillNotBeVacated =
          other.intent.position == placement.at || !canMove(other)
        !(targetPositionOccupied && targetWillNotBeVacated)
      }

  /** @param r_id
    * @return
    *   [[Event.MissionCompleted]] if the mission was active, [[None]] otherwise
    */
  def perform(r_id: RobotId): Option[Event] =
    for
      spot <- fleet.get(r_id)
      robot = spot.robot
      m_id <- robot.mission
      mission <- board.get(m_id)
    yield
      robot.release()
      board += (m_id -> mission.complete)
      Event.MissionCompleted(m_id)

  /** Advances all missions in the environment by one step.
    *
    * @return
    *   the sequence of generated [[Event]]s (e.g. mission failures)
    */
  def tick(): Seq[Event] =
    placements.foreach(_.robot.tick())
    val events = for
      mission <- missions.filterNot(_.isOver)
      next = mission.tick
    yield
      board += (mission.id -> next)
      if next.status == MissionStatus.Failed then
        releaseCarrier(mission)
        Some(Event.MissionFailed(mission.id))
      else None

    events.flatten

  /** Releases the carrier of a mission if it exists.
    */
  private def releaseCarrier(mission: Mission): Unit =
    for
      rid <- mission.carrier
      carrier <- robot(rid)
    do carrier.release()

  /** Automatically fails all [[Mission]]s that are not over yet, releasing the
    * robots which were carrying them.
    *
    * @return
    *   a [[Seq]] of [[Event.MissionFailed]] for all missions that are not over
    *   yet.
    */
  def end: Seq[Event] =
    for
      mission <- missions.filterNot(_.isOver)
      next = mission.fail
    yield
      board += (mission.id -> next)
      releaseCarrier(mission)
      Event.MissionFailed(mission.id)

  /** @return
    *   a snapshot of the current state of the simulation.
    */
  def snapshot: Snapshot = Snapshot(
    warehouse = warehouse,
    robots = placements.map(spot =>
      RobotSnapshot(spot.robot.id, spot.robot.status, spot.at)
    ),
    missions = missions
  )
