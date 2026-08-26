package it.unibo.sentinel.core.simulation

import it.unibo.sentinel.core.mission.MissionId
import it.unibo.sentinel.core.robot.RobotId
import it.unibo.sentinel.core.warehouse.Position

/** A simulation event, representing a change in the environment.
  */
enum Event:
  /** A mission has been assigned to a robot.
    * @param robot
    *   the robot that has been assigned a mission.
    * @param mission
    *   the mission that has been assigned to the robot.
    */
  case MissionAssigned(robot: RobotId, mission: MissionId)

  /** A mission has been completed.
    * @param mission
    *   the mission that has been completed.
    */
  case MissionCompleted(mission: MissionId)

  /** A mission has failed.
    * @param mission
    *   the mission that has failed.
    */
  case MissionFailed(mission: MissionId)

  /** A robot has been routed.
    * @param robot
    *   the robot that has been routed.
    * @param path
    *   the path that the robot has been routed along.
    */
  case RobotRouted(robot: RobotId, path: Seq[Position])

  /** A robot has moved.
    * @param robot
    *   the robot that has moved.
    * @param from
    *   the position from which the robot has moved.
    * @param to
    *   the position to which the robot has moved.
    */
  case RobotMoved(robot: RobotId, from: Position, to: Position)

  /** A robot has been blocked.
    * @param robot
    *   the robot that has been blocked.
    * @param at
    *   the position at which the robot has been blocked.
    */
  case RobotBlocked(robot: RobotId, at: Position)
