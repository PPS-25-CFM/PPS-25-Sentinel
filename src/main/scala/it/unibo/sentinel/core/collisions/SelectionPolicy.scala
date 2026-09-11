package it.unibo.sentinel.core.collisions

import scala.util.Random
import it.unibo.sentinel.core.robot.RobotId
import it.unibo.sentinel.core.scenario.Intent

/** Policy that defines how to select a winning [[Robot]] among colliding
  * intents.
  */
trait SelectionPolicy:

  /** @param intents
    *   list of conflicting [[Intent]]s to select from.
    * @return
    *   an `Option` containing the id of the selected [[Robot]].
    */
  def select(intents: Seq[Intent]): Option[RobotId]

object SelectionPolicy:

  /** Policy that selects the winning [[Robot]] randomly.
    *
    * @param rng
    *   the random generator used for selection.
    */
  def random(rng: Random): SelectionPolicy = intents =>
    val ids = intents.map(_.robotId)
    rng.shuffle(ids).headOption

  /** Policy that selects the [[Robot]] whose mission is closest to its
    * deadline.
    */
  def closestDeadline(): SelectionPolicy = intents =>
    val ordered =
      intents.sortBy(_.mission.map(_.deadline.value).getOrElse(Int.MaxValue))
    ordered.headOption.map(_.robotId)

  /** Policy that selects the [[Robot]] with the highest mission priority.
    */
  def highestPriority(): SelectionPolicy = intents =>
    val ordered =
      intents.sortBy(_.mission.map(_.priority.value).getOrElse(0)).reverse
    ordered.headOption.map(_.robotId)
