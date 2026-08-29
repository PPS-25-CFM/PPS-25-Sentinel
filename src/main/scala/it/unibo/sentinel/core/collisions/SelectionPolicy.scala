package it.unibo.sentinel.core.collisions

import scala.util.Random
import it.unibo.sentinel.core.robot.Robot
import it.unibo.sentinel.core.robot.RobotId

/** Policy that defines how to select/separate [[Robot]](s) that are colliding
  */
trait SelectionPolicy:

  /** @param robots
    *   list of [[Robot]]s to select a few from
    * @return
    *   a list containing the ids of the selected [[Robot]]s
    */
  def select(robots: Seq[Robot]): Seq[RobotId]

object SelectionPolicy:

  /** Policy that selects the [[Robot]](s) randomly
    *
    * @param selections
    *   number of [[Robot]]s to select
    */
  def random(selections: Int = 1): SelectionPolicy = robots =>
    val ids = robots.map(_.id)
    val selected = Random.shuffle(ids).take(selections)
    selected
