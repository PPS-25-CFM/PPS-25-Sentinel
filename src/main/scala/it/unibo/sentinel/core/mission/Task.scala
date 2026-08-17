package it.unibo.sentinel.core.mission

import it.unibo.sentinel.core.warehouse.Position

/** Represents the specific goal or action assigned within a Mission.
  *
  * A task can either be an active operational command or a terminal execution
  * state (Done or Fail).
  */
enum Task:
  /** Indicates that the task has been successfully completed. */
  case Done

  /** Indicates that the task execution has failed. */
  case Fail

  /** Represents an operational task requiring movement to a specific warehouse
    * position.
    */
  case Move(at: Position)

  /** Returns true if the task has been successfully completed. */
  def isDone: Boolean = this == Task.Done

  /** Returns true if the task execution has failed. */
  def isFail: Boolean = this == Task.Fail

  /** Returns the target warehouse position if this is a Move task, or None
    * otherwise.
    */
  def where: Option[Position] = this match
    case Move(at) => Some(at)
    case _        => None
