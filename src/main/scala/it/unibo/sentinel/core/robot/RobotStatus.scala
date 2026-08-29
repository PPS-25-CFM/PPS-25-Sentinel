package it.unibo.sentinel.core.robot

/** Represents a [[Robot]]'s operational status
  */
enum RobotStatus:

  /** The robot is standing with nothing to do
    */
  case Idle

  /** The robot is waiting for a signal to start executing the mission
    */
  case Ready

  /** The robot is executing a mission
    */
  case Moving

  /** The robot is waiting to resume its mission
    */
  case Waiting
