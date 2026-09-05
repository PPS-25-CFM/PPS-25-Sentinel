package it.unibo.sentinel.core.collisions

import it.unibo.sentinel.core.scenario.Placement
import it.unibo.sentinel.core.simulation.Event
import it.unibo.sentinel.core.robot.RobotStatus

private[collisions] final class PauseCollisionHandler extends BasicHandler:

  override def resolveCollisions(placements: Seq[Placement])(using
      selection: SelectionPolicy
  ): Seq[Event] =
    val directCollisions = pairCollisions(placements)
    val directEvents = pauseMoving(directCollisions)
    val unmovable =
      placements.filterNot(CollisionChecker.canMove(_, placements))
    val unmovableEvents = pauseMoving(unmovable)
    val movable = placements.filterNot(unmovable.toSet)
    val groupEvents = CollisionChecker
      .checkCollisions(movable.map(_.intent))
      .flatMap { group =>
        val colliding = movable.filter(p => group.contains(p.robot.id))
        val (selected, notSelected) = partition(colliding)
        pauseMoving(notSelected) ++ resumeWaiting(selected)
      }
    directEvents ++ unmovableEvents ++ groupEvents

  private def transitionRobot(
      placements: Seq[Placement],
      status: RobotStatus,
      action: Placement => Unit,
      toEvent: Placement => Event
  ): Seq[Event] =
    placements
      .filter(_.robot.status == status)
      .map { p =>
        action(p)
        toEvent(p)
      }

  private def pauseMoving(placements: Seq[Placement]): Seq[Event] =
    transitionRobot(
      placements,
      RobotStatus.Moving,
      _.robot.pause(),
      p => Event.RobotBlocked(p.robot.id, p.at)
    )

  private def resumeWaiting(placements: Seq[Placement]): Seq[Event] =
    transitionRobot(
      placements,
      RobotStatus.Waiting,
      _.robot.resume(),
      p => Event.RobotUnblocked(p.robot.id)
    )

  private def pairCollisions(placements: Seq[Placement]): Seq[Placement] =
    placements.filter { p =>
      placements.exists { other =>
        other.robot.id != p.robot.id &&
        other.at == p.intent.position &&
        other.intent.position == p.at
      }
    }
