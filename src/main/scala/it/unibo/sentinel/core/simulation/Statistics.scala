package it.unibo.sentinel.core.simulation

import it.unibo.sentinel.core.robot.RobotId
import it.unibo.sentinel.core.mission.MissionId
import it.unibo.sentinel.core.simulation.Event.*

import scala.reflect.ClassTag
import it.unibo.sentinel.core.scenario.Scenario

object Statistics:
  /** The report of a [[Simulation]] run.
    */
  trait Report:
    /** @return
      *   how many ticks have been executed in the [[Simulation]] run.
      */
    def ticks: Int

    /** @return
      *   the number of [[Robot]]s deployed in the [[Simulation]] run.
      */
    def numOfRobots: Int

    /** @return
      *   the number of missions loaded, including pending and ongoing ones.
      */
    def numOfMissions: Int

    /** @return
      *   the number of [[Mission]]s completed in the [[Simulation]] run.
      */
    def numOfCompletedMissions: Int

    /** @return
      *   the number of [[Mission]]s failed in the [[Simulation]] run.
      */
    def numOfFailedMissions: Int

    /** Measures how many [[Mission]]s have been completed per [[Mission]] in
      * the [[Simulation]] run.
      *
      * @return
      *   the completion rate of the [[Mission]]s in the [[Simulation]] run.
      *   None if no missions are loaded.
      */
    def completionRate: Option[Double] =
      rate(numOfCompletedMissions, numOfMissions)

    /** Measures how many [[Mission]]s have been completed per [[Tick]] in the
      * [[Simulation]] run.
      *
      * @return
      *   the throughput of the [[Mission]]s in the [[Simulation]] run. None if
      *   no ticks have been executed.
      */
    def throughput: Option[Double] =
      rate(numOfCompletedMissions, ticks)

    /** Measures how many [[Mission]]s have been completed per [[Tick]] and per
      * [[Robot]] in the [[Simulation]] run.
      *
      * @return
      *   the throughput of the [[Mission]]s per [[Robot]] in the [[Simulation]]
      *   run. None if no ticks have been executed or no robots are deployed.
      */
    def throughputPerRobot: Option[Double] =
      rate(numOfCompletedMissions, ticks * numOfRobots)

    /** Measures the average number of [[Tick]]s to complete a [[Mission]] from
      * the moment it has been assigned. Both assignment and completion ticks
      * are included, so completion in the same tick takes one tick.
      *
      * @return
      *   the average number of [[Tick]]s to complete a [[Mission]] in the
      *   [[Simulation]] run. None if no completed mission has an assignment.
      */
    def averageCompletionTime: Option[Double]

    /** Measures total distance traveled per completed mission, including
      * distance traveled for failed and ongoing missions.
      *
      * @return
      *   the average distance traveled by all [[Robot]]s to complete a
      *   [[Mission]] in the [[Simulation]] run. None if none have completed.
      */
    def averageDistancePerMission: Option[Double] =
      rate(totalDistance, numOfCompletedMissions)

    /** Measures the maximum duration from assignment to successful completion.
      * Both assignment and completion ticks are included.
      *
      * @return
      *   the maximum number of [[Tick]]s to complete a [[Mission]] in the
      *   [[Simulation]] run. None if no completed mission has an assignment.
      */
    def maxCompletionTime: Option[Int]

    /** @return
      *   the total distance traveled by all [[Robot]]s in the [[Simulation]]
      *   run, measured as the number of movement events (one cell per move).
      */
    def totalDistance: Int

    /** @return
      *   the number of block events, not the number of ticks spent blocked.
      */
    def numOfBlocks: Int

    /** Usage is measured by the number of assigned missions, regardless of
      * their outcome. All robots tied for the maximum are included.
      *
      * @return
      *   a [[Set]] of [[RobotId]]s corresponding to the most used [[Robot]]s in
      *   the [[Simulation]] run, or an empty set if no robots are deployed.
      */
    def mostUsedRobot: Set[RobotId]

    /** Usage is measured by the number of assigned missions, including robots
      * with zero assignments. All robots tied for the minimum are included.
      *
      * @return
      *   a [[Set]] of [[RobotId]]s corresponding to the least used [[Robot]]s
      *   in the [[Simulation]] run, or an empty set if no robots are deployed.
      */
    def leastUsedRobot: Set[RobotId]

  /** Builds a report from a snapshot and the complete, chronological history of
    * the same simulation run. Each mission is assigned at most once and has at
    * most one outcome.
    *
    * @param scenario
    *   the [[Scenario]] the [[Simulation]] has been run on.
    * @param history
    *   the [[History]] of the [[Simulation]].
    * @param at
    *   the number of executed steps, as returned by [[Simulation.time]].
    * @return
    *   the [[Report]] of the [[Simulation]] run.
    */
  def report(scenario: Scenario, history: History, at: Tick): Report =
    Registry(scenario, history, at)

  private final case class Registry(
      scenario: Scenario,
      history: History,
      tick: Tick
  ) extends Report:

    override lazy val ticks: Int = tick.value

    override lazy val numOfRobots: Int = scenario.spawns.size

    override lazy val numOfMissions: Int = scenario.missions.size

    override lazy val numOfCompletedMissions: Int =
      countEvents[MissionCompleted]

    override lazy val numOfFailedMissions: Int =
      countEvents[MissionFailed]

    override lazy val totalDistance: Int =
      countEvents[RobotMoved]

    override lazy val numOfBlocks: Int =
      countEvents[RobotBlocked]

    override lazy val averageCompletionTime: Option[Double] =
      rate(completionTimes.map(_._2).sum, completionTimes.size)

    override lazy val maxCompletionTime: Option[Int] =
      completionTimes.map(_._2).maxOption

    override lazy val mostUsedRobot: Set[RobotId] =
      robotsByUsage(_.maxOption)

    override lazy val leastUsedRobot: Set[RobotId] =
      robotsByUsage(_.minOption)

    private def robotsByUsage(
        select: Iterable[Int] => Option[Int]
    ): Set[RobotId] =
      val numOfAssignments = assignments.values.map(_.size)
      select(numOfAssignments) match
        case None        => Set.empty
        case Some(count) =>
          assignments
            .collect:
              case (robot, missions) if missions.size == count => robot
            .toSet

    private lazy val completionTimes: Seq[(MissionId, Int)] =
      val assigmentTimes = history
        .collect:
          case (MissionAssigned(_, mission), at) => (mission, at.value)
        .toMap
      history.flatMap:
        case (MissionCompleted(mission), completedAt) =>
          assigmentTimes
            .get(mission)
            .map: assignedAt =>
              val duration = completedAt.value - assignedAt
              (mission, duration)
        case _ => None

    private lazy val assignments: Map[RobotId, Set[MissionId]] =
      val allRobots = scenario.spawns.map(_.id)
      val base = Map.from(allRobots.map((_, Set.empty[MissionId])))
      val fromHistory = history
        .collect:
          case (MissionAssigned(robot, mission), _) => (robot, mission)
        .groupMap(_._1)(_._2)
        .map((robot, missions) => (robot, missions.toSet))
      base ++ fromHistory

    private def countEvents[E <: Event: ClassTag]: Int =
      val clazz = summon[ClassTag[E]].runtimeClass
      history.count((event, _) => clazz.isInstance(event))

  private def rate(num: Double, den: Double): Option[Double] =
    Option.unless(den == 0d)(num / den)
