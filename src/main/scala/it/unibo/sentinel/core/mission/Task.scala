package it.unibo.sentinel.core.mission

import it.unibo.sentinel.core.warehouse.Position

/** Represents the specific goal or action assigned within a Mission.
  *
  * A task can either be an active operational command or a terminal execution
  * state (Done or Fail).
  */
enum Task:
  /** Represents a successfully completed task */
  case Done

  /** Represents a failed task. */
  case Fail

  /** Represents an operational task requiring movement to a specific warehouse
    * position.
    *
    * @param at
    *   The target destination to reach
    */
  case Move(at: Position)

  /** @return
    *   whether the task is completed.
    */
  def isDone: Boolean = this == Task.Done

  /** @return
    *   whether the task is failed.
    */
  def isFail: Boolean = this == Task.Fail

  /** @return
    *   the target warehouse position if this is a Move task, or None otherwise.
    */
  def where: Option[Position] = this match
    case Move(at) => Some(at)
    case _        => None
