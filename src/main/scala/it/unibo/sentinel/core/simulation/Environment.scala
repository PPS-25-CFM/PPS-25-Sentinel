package it.unibo.sentinel.core.simulation

import it.unibo.sentinel.core.robot.RobotId
import it.unibo.sentinel.core.scenario.Placement
import it.unibo.sentinel.core.warehouse.Warehouse
import it.unibo.sentinel.core.mission.{Mission, MissionId}
import it.unibo.sentinel.core.routing.Path
import it.unibo.sentinel.core.mission.MissionStatus

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
  def robot(r_id: RobotId): Option[Robot] =
    placements.find(_.robot.id == r_id).map(_.robot)

  /** @param m_id
    *   the [[MissionId]] of the mission to query
    * @return
    *   the [[Mission]] with the given id, if any.
    */
  def mission(m_id: MissionId): Option[Mission] =
    missions.find(_.id == m_id)

  /** @param robotId
    *   the [[RobotId]] of the robot contained in the placement to query.
    * @return
    *   the [[Placement]] with the given id, if any.
    */
  def placement(r_id: RobotId): Option[Placement] =
    placements.find(_.robot.id == r_id)

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
      Event.RobotRouted(r_id, path)

  /** @param r_id
    * @return
    *   an [[Event]] if the [[Robot]] was able to move, None otherwise
    */
  def advance(r_id: RobotId): Option[Event] =
    for
      spot <- fleet.get(r_id)
      robot = spot.robot
      from = spot.at
      to <- robot.next
    yield
      if !fleet.values.exists(_.at == to)
      then
        robot.step()
        fleet = fleet + (r_id -> spot.copy(at = to))
        Event.RobotMoved(r_id, from, to)
      else Event.RobotBlocked(r_id, from)
