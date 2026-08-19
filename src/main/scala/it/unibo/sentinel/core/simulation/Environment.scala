package it.unibo.sentinel.core.simulation

import it.unibo.sentinel.core.robot.RobotId
import it.unibo.sentinel.core.scenario.Placement
import it.unibo.sentinel.core.warehouse.Warehouse
import it.unibo.sentinel.core.mission.{Mission, MissionId}

private[core] final class Environment private[core](
  val warehouse: Warehouse,
  private val fleet: Map[RobotId, Placement],
  private var board: Map[MissionId, Mission]
):

  def placements: Seq[Placement] = fleet.values.toSeq
  def missions: Seq[Mission] = board.values.toSeq

  def assign(r_id: RobotId, m_id: MissionId): Unit =
    for
      place <- fleet.get(r_id)
      robot = place.robot
      mission <- board.get(m_id)
    do
      robot.accept(m_id)
      board = board + (m_id -> mission.assignTo(r_id))
