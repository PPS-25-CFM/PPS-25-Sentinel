package it.unibo.sentinel.core.collisions

import it.unibo.sentinel.core.robot.RobotId
import it.unibo.sentinel.core.scenario.Intent
import it.unibo.sentinel.core.scenario.Placement
import it.unibo.sentinel.core.robot.RobotStatus
import it.unibo.sentinel.core.simulation.Tick

/** Used to check for collisions between [[Robot]]s
  */
trait CollisionChecker:

  /** @param intents
    *   the intent of each robot to move to a specific position
    * @return
    *   a list of groups of [[RobotId]]s, where each group represents the robots
    *   that will collide (intend to move to the same position)
    */
  def checkCollisions(intents: Seq[Intent]): Seq[Seq[RobotId]]

  /** @param placement
    *   the placement of the robot to check
    * @param placements
    *   group of placements to check against
    * @return
    *   `true` if the robot can move to its intended position, `false` otherwise
    */
  def canMove(placement: Placement, placements: Seq[Placement]): Boolean

  /** @param placements
    *   the placements of robots to check for direct collisions.
    * @return
    *   a list of placements that are colliding between each other in pairs (one
    *   against the other).
    */
  def colliding(placements: Seq[Placement]): Seq[(Placement, Placement)]

object CollisionChecker extends CollisionChecker:

  override def checkCollisions(intents: Seq[Intent]): Seq[Seq[RobotId]] =
    intents
      .groupBy(_.position)
      .map(_._2.map(_.robotId))
      .toSeq

  override def canMove(placement: Placement, fleet: Seq[Placement]): Boolean =
    placement.robot.status == RobotStatus.Moving &&
      fleet.filterNot(_ == placement).forall { other =>
        val targetOccupied = other.at == placement.intent.position
        lazy val targetBlocked =
          other.robot.remaining == Tick.zero ||
            other.intent.position == placement.at ||
            !canMove(other, fleet)
        !(targetOccupied && targetBlocked)
      }

  override def colliding(
      placements: Seq[Placement]
  ): Seq[(Placement, Placement)] =
    for
      (placement, index) <- placements.zipWithIndex
      collider <- placements
        .drop(index + 1)
        .find: other =>
          other.robot.id != placement.robot.id &&
            other.at == placement.intent.position &&
            other.intent.position == placement.at
    yield (placement, collider)
