package it.unibo.sentinel.core.collisions

import scala.util.Random
import it.unibo.sentinel.core.robot.Robot
import it.unibo.sentinel.core.robot.RobotId
import it.unibo.sentinel.core.mission.Mission

/** Policy that defines how to select/separate [[Robot]](s) that are colliding
  */
trait SelectionPolicy:

  /** @param robots
    *   list of [[Robot]]s to select a few from
    * @return
    *   an `Option` containing the id of the selected [[Robot]]
    */
  def select(robots: Seq[Robot]): Option[RobotId]

object SelectionPolicy:

  /** Policy that selects the [[Robot]](s) randomly
    *
    * @param rng
    *   the random generator used for the selection.
    */
  def random(rng: Random): SelectionPolicy = robots =>
    val ids = robots.map(_.id)
    rng.shuffle(ids).headOption

  /** Policy that selects the [[Robot]](s) based on who has the mission closest
    * to failing.
    *
    * @param selections
    *   number of [[Robot]]s to select.
    * @param missions
    *   list of [[Mission]]s to extract the deadline from.
    */
  def closestDeadline()(using missions: => Seq[Mission]): SelectionPolicy =
    selectByMissionProperty(_.deadline)

  def highestPriority()(using missions: => Seq[Mission]): SelectionPolicy =
    selectByMissionProperty(_.priority, false)

  private def selectByMissionProperty[A: Ordering](
      extractProperty: Mission => A,
      ascending: Boolean = true
  )(using missions: => Seq[Mission]): SelectionPolicy = robots =>
    val result = robots.flatMap { r =>
      for
        missionId <- r.mission
        mission <- missions.find(_.id == missionId)
      yield (r.id, extractProperty(mission))
    }
    val sorted =
      if ascending then result.sortBy(_._2)
      else result.sortBy(_._2)(using Ordering[A].reverse)
    sorted.map(_._1).headOption
