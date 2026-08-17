package it.unibo.sentinel.core.robot

/** */
trait Robot:
  /** @return the robot's identifier
    */
  def id: RobotId

object Robot:
  def apply(robotId: RobotId): Robot = new Robot:
    override def id: RobotId = robotId