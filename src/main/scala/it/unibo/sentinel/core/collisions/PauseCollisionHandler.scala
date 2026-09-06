package it.unibo.sentinel.core.collisions

import it.unibo.sentinel.core.scenario.Placement
import it.unibo.sentinel.core.simulation.Event
import it.unibo.sentinel.core.robot.RobotStatus
import it.unibo.sentinel.core.scenario.Intent
import it.unibo.sentinel.core.collisions.CollisionChecker.canMove
import it.unibo.sentinel.core.warehouse.Position
import it.unibo.sentinel.core.robot.RobotId

private[collisions] final class PauseCollisionHandler extends BasicHandler:

  override def resolveCollisions(placements: Seq[Placement])(using
      selection: SelectionPolicy
  ): Seq[Event] =
    val faceToFace =
      CollisionChecker.colliding(placements).flatMap((p1, p2) => Seq(p1, p2))
    val faceToFaceEvents = blockMoving(faceToFace)
    val faceToFaceIntents = faceToFace.map(p => Intent(p.robot.id, p.at))
    val remaining = placements.diff(faceToFace)
    val groupEvents = CollisionChecker
      .checkCollisions(faceToFaceIntents ++ remaining.map(_.intent))
      .flatMap: (target, group) =>
        resolveIndirectCollisions(placements, target, group)
    faceToFaceEvents ++ groupEvents

  private def resolveIndirectCollisions(
      placements: Seq[Placement],
      target: Position,
      group: Seq[RobotId]
  )(using selection: SelectionPolicy): Seq[Event] =
    val robots = placements.filter(p => group.contains(p.robot.id))
    if robots.exists(p => p.at == target) then
      blockMoving(robots.filterNot(p => p.intent.position == p.at))
    else
      val moveable = robots.filter(p => canMove(p, placements))
      val (selected, notSelected) = partition(moveable)
      blockMoving(notSelected) ++ resumeWaiting(selected)

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
