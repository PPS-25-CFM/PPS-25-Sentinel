package it.unibo.sentinel.core.collisions

import it.unibo.sentinel.core.scenario.Placement
import it.unibo.sentinel.core.simulation.Event
import it.unibo.sentinel.core.robot.RobotStatus

private[collisions] final class PauseCollisionHandler extends BasicHandler:

  override def resolveCollisions(placements: Seq[Placement])(using
      selection: SelectionPolicy
  ): Seq[Event] =
    val directCollisions = CollisionChecker.colliding(placements)
    val directEvents = blockMoving(directCollisions.flatMap((p1, p2) => Seq(p1, p2)))
    val unmovable =
      placements.filterNot(CollisionChecker.canMove(_, placements))
    val unmovableEvents = blockMoving(unmovable)
    val movable = placements.filterNot(unmovable.toSet)
    val groupEvents = CollisionChecker
      .checkCollisions(movable.map(_.intent))
      .flatMap { group =>
        val colliding = movable.filter(p => group.contains(p.robot.id))
        val (selected, notSelected) = partition(colliding)
        blockMoving(notSelected) ++ resumeWaiting(selected)
      }
    directEvents ++ unmovableEvents ++ groupEvents

  private def resumeWaiting(placements: Seq[Placement]): Seq[Event] =
    transitionRobot(
      placements,
      RobotStatus.Waiting,
      _.robot.resume(),
      p => Event.RobotUnblocked(p.robot.id)
    )

  private def blockMoving(placements: Seq[Placement]): Seq[Event] =
    transitionRobot(
      placements,
      RobotStatus.Moving,
      _.robot.pause(),
      p => Event.RobotBlocked(p.robot.id, p.at)
    )
