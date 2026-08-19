package it.unibo.sentinel.core.simulation

import it.unibo.sentinel.core.robot.RobotId
import it.unibo.sentinel.core.scenario.Placement
import it.unibo.sentinel.core.warehouse.Warehouse
import it.unibo.sentinel.core.mission.{Mission, MissionId}

final class Environment (
  val warehouse: Warehouse,
  var fleet: Map[RobotId, Placement],
  var board: Map[MissionId, Mission]
):

  def placements: Iterable[Placement] = fleet.values
  def missions: Iterable[Mission] = board.values
