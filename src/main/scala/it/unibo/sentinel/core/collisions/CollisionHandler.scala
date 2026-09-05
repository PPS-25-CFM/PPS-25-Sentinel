package it.unibo.sentinel.core.collisions

import it.unibo.sentinel.core.simulation.Event
import it.unibo.sentinel.core.robot.RobotStatus
import it.unibo.sentinel.core.scenario.Placement
import it.unibo.sentinel.core.simulation.Tick

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
        val directCollisions = pairCollisions(placements)
        var events: Seq[Event] = actAndCreateEvent(directCollisions)(
          _.robot.pause(),
          p => p.robot.status == RobotStatus.Moving,
          p => Event.RobotBlocked(p.robot.id, p.at)
        )
        val unmovable = placements.filterNot(canMove(_, placements))
        events = events ++ actAndCreateEvent(unmovable)(
          _.robot.pause(),
          p => p.robot.status == RobotStatus.Moving,
          p => Event.RobotBlocked(p.robot.id, p.at)
        )
        val remaining = placements.filterNot(unmovable.contains)
        val collisions =
          CollisionChecker.checkCollisions(remaining.map(_.intent))
        for
          group <- collisions
          colliding = remaining.filter(p => group.contains(p.robot.id))
          (selected, notSelected) = partition(colliding)
        do
          events = events ++ actAndCreateEvent(notSelected)(
            _.robot.pause(),
            p => p.robot.status == RobotStatus.Moving,
            p => Event.RobotBlocked(p.robot.id, p.at)
          ) ++ actAndCreateEvent(selected)(
            _.robot.resume(),
            p => p.robot.status == RobotStatus.Waiting,
            p => Event.RobotUnblocked(p.robot.id)
          )
        events

      private def actAndCreateEvent(on: Seq[Placement])(
          action: Placement => Unit,
          cond: Placement => Boolean,
          event: Placement => Event
      ): Seq[Event] =
        on.filter(cond).map { p =>
          action(p)
          event(p)
        }

      private def pairCollisions(placements: Seq[Placement]): Seq[Placement] =
        placements
          .filter { p =>
            placements.exists(other =>
              other.robot.id != p.robot.id &&
                other.at == p.intent.position && other.intent.position == p.at
            )
          }

      private def canMove(
          placement: Placement,
          fleet: Seq[Placement]
      ): Boolean =
        placement.robot.status == RobotStatus.Moving
          && fleet.filterNot(_ == placement).forall { other =>
            val targetPositionOccupied = other.at == placement.intent.position
            lazy val targetWillNotBeVacated =
              other.robot.remaining == Tick.zero || other.intent.position == placement.at || !canMove(
                other,
                fleet
              )
            !(targetPositionOccupied && targetWillNotBeVacated)
          }
