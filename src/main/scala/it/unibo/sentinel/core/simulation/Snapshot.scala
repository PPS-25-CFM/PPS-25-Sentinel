package it.unibo.sentinel.core.simulation

import it.unibo.sentinel.core.robot.RobotId
import it.unibo.sentinel.core.warehouse.Position
import it.unibo.sentinel.core.warehouse.Warehouse
import it.unibo.sentinel.core.mission.Mission
import it.unibo.sentinel.core.robot.RobotStatus

/** Represents the state of a robot at a given time in the simulation.
  *
  * @param id
  *   the [[RobotId]] of the robot.
  * @param status
  *   the [[RobotStatus]] of the robot.
  * @param position
  *   the [[Position]] of the robot in the warehouse.
  */
final case class RobotSnapshot(
    id: RobotId,
    status: RobotStatus,
    position: Position
)

/** Represents a snapshot of the simulation at a given time.
  *
  * @param warehouse
  *   the static layout of the warehouse.
  * @param robots
  *   the snapshots of all robots in the simulation.
  * @param missions
  *   the missions in the simulation.
  */
final case class Snapshot(
    warehouse: Warehouse,
    robots: Seq[RobotSnapshot],
    missions: Seq[Mission]
)
