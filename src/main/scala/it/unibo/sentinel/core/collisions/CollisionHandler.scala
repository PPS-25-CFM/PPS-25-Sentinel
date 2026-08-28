package it.unibo.sentinel.core.collisions

import it.unibo.sentinel.core.simulation.Event
import it.unibo.sentinel.core.robot.RobotStatus
import it.unibo.sentinel.core.scenario.Placement

/** Defines how to handle collisions between [[Robot]]s
  */
trait CollisionHandler:
  /** Resolves collisions between a group of [[Robot]]s
    *
    * @param placements
    *   list of colliding [[Robot]]s
    * @param selection
    *   used to select the [[Robot]]s to apply the resolution to
    */
  def resolveCollisions(placements: Seq[Placement])(using
      selection: SelectionPolicy
  ): Seq[Event]

private abstract class BasicHandler extends CollisionHandler:

  /** Partitions a list of robots.
    *
    * @param placements
    *   the robots to partition.
    * @param selection
    *   the selection policy that determines how to partition.
    * @return
    *   a tuple of two list of robots, the first are the selected ones, the
    *   second are the others.
    */
  protected def partition(placements: Seq[Placement])(using
      selection: SelectionPolicy
  ): (Seq[Placement], Seq[Placement]) =
    val robots = placements.map(_.robot)
    val selectedIds = selection.select(robots).toSet
    placements.find(p => p.at == p.intent.position) match
      case Some(standing) =>
        (Seq(standing), placements.filterNot(_ == standing))
      case None =>
        placements.partition(p => selectedIds.contains(p.robot.id))

object CollisionHandler:

  /** Handler based on pausing [[Robot]]s
    *
    * @param selectionPolicy
    *   policy used to select the [[Robot]](s) that can move
    */
  def pausing(): CollisionHandler =
    new BasicHandler:
      override def resolveCollisions(placements: Seq[Placement])(using
          selection: SelectionPolicy
      ): Seq[Event] =
        val (selected, notSelected) = partition(placements)
        val blocked = notSelected
          .filter(_.robot.status == RobotStatus.Moving)
          .map(p => Event.RobotBlocked(p.robot.id, p.at))
        val resumed = selected
          .filter(_.robot.status == RobotStatus.Waiting)
          .map(p => Event.RobotUnblocked(p.robot.id))
        selected.foreach(_.robot.resume())
        notSelected.foreach(_.robot.pause())
        blocked ++ resumed
