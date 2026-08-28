package it.unibo.sentinel.core.mission

import it.unibo.sentinel.core.warehouse.Position

/** Represents an operational task to be performed within a mission.
  */
sealed trait Task:
  /** @return
    *   The target [[Position]] associated with the underlying step or sequence.
    */
  def destination: Position

/** Companion object for [[Task]], providing factory methods for task creation.
  */
object Task:

  private final case class Single(step: Action) extends Task:
    override def destination: Position = step.targetPosition

  /** @param destination
    *   The target [[Position]] to reach.
    * @return
    *   A new [[Task]] targeting the given position.
    */
  def goto(destination: Position): Task = Single(Action.Move(destination))
