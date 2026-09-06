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

  /** Policy that selects the [[Robot]](s) based on who has the mission closest
    * to failing.
    *
    * @param selections
    *   number of [[Robot]]s to select.
    * @param missions
    *   list of [[Mission]]s to extract the deadline from.
    */
  def closestDeadline(
      selections: Int = 1
  )(using missions: => Seq[Mission]): SelectionPolicy = robots =>
    robots
      .flatMap { r =>
        for
          missionId <- r.mission
          mission <- missions.find(_.id == missionId)
        yield (r.id, mission.deadline)
      }
      .sortBy(_._2)
      .map(_._1)
      .take(selections)
