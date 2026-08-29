package it.unibo.sentinel.core.collisions

import it.unibo.sentinel.core.robot.RobotId
import it.unibo.sentinel.core.scenario.Intent

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

object CollisionChecker extends CollisionChecker:

  override def checkCollisions(intents: Seq[Intent]): Seq[Seq[RobotId]] =
    intents
      .groupBy(_.position)
      .map(_._2.map(_.robotId))
      .toSeq
