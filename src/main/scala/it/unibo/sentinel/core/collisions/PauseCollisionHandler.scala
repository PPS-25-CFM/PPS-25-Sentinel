package it.unibo.sentinel.core.collisions

import it.unibo.sentinel.core.scenario.Placement
import it.unibo.sentinel.core.simulation.Event
import it.unibo.sentinel.core.robot.RobotStatus
import it.unibo.sentinel.core.scenario.Intent
import it.unibo.sentinel.core.collisions.CollisionChecker.canMove

private[collisions] final class PauseCollisionHandler extends BasicHandler:

  override def resolveCollisions(placements: Seq[Placement])(using
      selection: SelectionPolicy
  ): Seq[Event] =
    // Block robots that collide face-to-face
    val directCollisions =
      CollisionChecker.colliding(placements).flatMap((p1, p2) => Seq(p1, p2))
    val directEvents = blockMoving(directCollisions)

    val blockedIntents = directCollisions.map(p => Intent(p.robot.id, p.at))
    val others = placements.diff(directCollisions)
    val groupEvents = CollisionChecker
      .checkCollisions(blockedIntents ++ others.map(_.intent))
      .flatMap { group =>
        val events = for
          robots = placements.filter(p => group.contains(p.robot.id))
          ex <- robots.headOption
          target = ex.intent
        yield
          if robots.exists(p => p.at == target.position) then blockMoving(robots)
          else
            val moveable = robots.filter(p => canMove(p, placements))
            val (selected, notSelected) = partition(moveable)
            blockMoving(notSelected) ++ resumeWaiting(selected)
        events.getOrElse(Seq())
      }
    directEvents ++ groupEvents

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
