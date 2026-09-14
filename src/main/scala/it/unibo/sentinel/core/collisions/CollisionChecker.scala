package it.unibo.sentinel.core.collisions

import it.unibo.sentinel.core.robot.RobotId
import it.unibo.sentinel.core.scenario.Intent
import it.unibo.sentinel.core.warehouse.Position

case class IndirectCollision(target: Position, robots: Seq[RobotId])
case class DirectCollision(robot1: RobotId, robot2: RobotId)

/** Used to check for collisions between [[Robot]]s
  */
trait CollisionChecker:

  /** @param intents
    *   the [[Intent]] of each [[Robot]] to move to a specific position
    * @return
    *   a list of groups of [[RobotId]]s, where each group represents the robots
    *   that will collide (intend to move) in the same [[Position]].
    */
  def indirectCollisions(intents: Seq[Intent]): Seq[IndirectCollision]

  /** @param intents
    *   the [[Intent]]s of [[Robots]] to check for direct collisions.
    * @return
    *   a list of pairs of the ids of the [[Robot]]s that are colliding between
    *   each other (head to head).
    */
  def directCollisions(intents: Seq[Intent]): Seq[DirectCollision]

object CollisionChecker extends CollisionChecker:

  override def indirectCollisions(
      intents: Seq[Intent]
  ): Seq[IndirectCollision] =
    intents
      .groupBy(_.to)
      .map(x => IndirectCollision(x._1, x._2.map(_.robotId).toSeq))
      .toSeq

  override def directCollisions(
      intents: Seq[Intent]
  ): Seq[DirectCollision] =
    val collisions = for
      (current, index) <- intents.zipWithIndex
      collider <- intents
        .drop(index + 1)
        .find: other =>
          other.robotId != current.robotId &&
            other.from == current.to &&
            other.to == current.from
    yield DirectCollision(current.robotId, collider.robotId)
    collisions.toSeq
