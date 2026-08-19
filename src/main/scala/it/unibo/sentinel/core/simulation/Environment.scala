package it.unibo.sentinel.core.simulation

import it.unibo.sentinel.core.robot.RobotId
import it.unibo.sentinel.core.scenario.Placement
import it.unibo.sentinel.core.warehouse.Warehouse
import it.unibo.sentinel.core.mission.{Mission, MissionId}
import it.unibo.sentinel.core.routing.Path

/** Provides query operations to inspect the state of the simulation.
  */
trait Queries:
  import it.unibo.sentinel.core.mission.MissionStatus.*
  import it.unibo.sentinel.core.robot.RobotStatus

  /** @return
    *   the current [[Placement]]s of all robots in the simulation.
    */
  def placements: Seq[Placement]

  /** @return
    *   all [[Mission]]s present in the simulation.
    */
  def missions: Seq[Mission]

  /** @param status
    *   the [[RobotStatus]] to filter robots by.
    * @return
    *   the [[Placement]]s of robots that currently have the given
    *   [[RobotStatus]].
    */
  def standing(status: RobotStatus): Seq[Placement] =
    placements.filter(_.robot.status == status).toSeq

  /** @return
    *   all [[Mission]]s currently in [[Pending]] status.
    */
  def pendingMissions: Seq[Mission] =
    missions.filter(_.status == Pending).toSeq

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

  /** Assigns a mission to a robot, notifying the robot and updating the mission
    * status.
    *
    * @param r_id
    *   the [[RobotId]] of the target robot.
    * @param m_id
    *   the [[MissionId]] of the mission to assign.
    */
  def assign(r_id: RobotId, m_id: MissionId): Unit =
    for
      place <- fleet.get(r_id)
      robot = place.robot
      mission <- board.get(m_id)
    do
      robot.accept(m_id)
      board = board + (m_id -> mission.assignTo(r_id))

  /** Instructs a robot to follow a planned routing path.
    *
    * @param r_id
    *   the [[RobotId]] of the robot.
    * @param path
    *   the [[Path]] to follow.
    */
  def route(r_id: RobotId, path: Path): Unit =
    for
      spot <- fleet.get(r_id)
      robot = spot.robot
    do robot.follow(path)

  /** Advances the robot to its next position along its assigned path, provided
    * that the target position is not currently occupied by another robot.
    *
    * @param r_id
    *   the [[RobotId]] of the robot to step forward.
    */
  def advance(r_id: RobotId): Unit =
    for
      spot <- fleet.get(r_id)
      robot = spot.robot
      to <- robot.next
    do
      if !fleet.values.exists(_.at == to) then
        robot.step()
        fleet = fleet + (r_id -> spot.copy(at = to))
