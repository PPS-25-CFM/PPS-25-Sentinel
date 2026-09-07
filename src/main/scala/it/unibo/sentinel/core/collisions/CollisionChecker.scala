package it.unibo.sentinel.core.collisions

import it.unibo.sentinel.core.robot.RobotId
import it.unibo.sentinel.core.scenario.Intent
import it.unibo.sentinel.core.scenario.Placement
import it.unibo.sentinel.core.simulation.Tick
import it.unibo.sentinel.core.warehouse.Position

/** Used to check for collisions between [[Robot]]s
  */
trait CollisionChecker:

  /** @param intents
    *   the intent of each robot to move to a specific position
    * @return
    *   a list of groups of [[RobotId]]s, where each group represents the robots
    *   that will collide (intend to move to the same position)
    */
  def indirectCollisions(intents: Seq[Intent]): Map[Position, Seq[RobotId]]

  /** @param placements
    *   the placements of robots to check for direct collisions.
    * @return
    *   a list of placements that are colliding between each other in pairs (one
    *   against the other).
    */
  def directCollisions(intents: Seq[Intent]): Seq[(RobotId, RobotId)]

  /** @param placement
    *   the placement of the robot to check
    * @param placements
    *   group of placements to check against
    * @return
    *   `true` if the robot can move to its intended position, `false` otherwise
    */
  def canMove(placement: Placement, placements: Seq[Placement]): Boolean

object CollisionChecker extends CollisionChecker:

  override def indirectCollisions(
      intents: Seq[Intent]
  ): Map[Position, Seq[RobotId]] =
    intents
      .groupBy(_.to)
      .map(x => (x._1, x._2.map(_.robotId)))

  override def directCollisions(
      intents: Seq[Intent]
  ): Seq[(RobotId, RobotId)] =
    for
      (current, index) <- intents.zipWithIndex
      collider <- intents
        .drop(index + 1)
        .find: other =>
          other.robotId != current.robotId &&
            other.from == current.to &&
            other.to == current.from
    yield (current.robotId, collider.robotId)

  override def canMove(placement: Placement, fleet: Seq[Placement]): Boolean =
    placement.intent.from != placement.intent.to &&
      fleet.filterNot(_ == placement).forall { other =>
        val targetOccupied = other.at == placement.intent.to
        lazy val targetBlocked =
          other.intent.to == placement.at ||
            !canMove(other, fleet)
        !(targetOccupied && (other.robot.remaining != Tick.zero || targetBlocked))
      }
