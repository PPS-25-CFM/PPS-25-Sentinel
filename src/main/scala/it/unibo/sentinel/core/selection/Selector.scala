package it.unibo.sentinel.core.selection

import it.unibo.sentinel.core.mission.Mission
import it.unibo.sentinel.core.robot.Robot
import it.unibo.sentinel.core.warehouse.Position

/** Represents a robot's spatial context.
  *
  * @param robot
  *   The robot candidate.
  * @param at
  *   The current warehouse position of the robot.
  */
final case class Placement(
    robot: Robot,
    at: Position
)

/** Domain strategy interface for selecting the best suitable candidate
  * placement for a given mission.
  */
trait Selector:

  /** @param mission
    *   The mission to be assigned.
    * @param among
    *   The collection of candidate placements.
    * @return
    *   [[Some]] selected [[Placement]] if an available candidate satisfies the
    *   strategy, or [[None]] if no candidates are available or suitable.
    */
  def choose(
      mission: Mission,
      among: Iterable[Placement]
  ): Option[Placement] =
    val available = among.filter(_.robot.canAccept)
    if available.isEmpty then None
    else selectFromAvailable(mission, available)

  /** @param mission
    *   The mission to be assigned.
    * @param available
    *   The pre-filtered collection of available candidate placements.
    * @return
    *   [[Some]] chosen [[Placement]], or [[None]] if no suitable candidate is
    *   found.
    */
  protected def selectFromAvailable(
      mission: Mission,
      available: Iterable[Placement]
  ): Option[Placement] = ???

object Selector:

  import it.unibo.sentinel.core.routing.Navigator

  /** A proximity-based selection strategy that assigns the mission to the
    * available candidate closest to the mission's current target.
    *
    * @param navigator
    *   The spatial navigation routing engine used to compute distances.
    */
  case class Nearest(navigator: Navigator) extends Selector:

    /** @param mission
      *   The mission whose destination is evaluated.
      * @param available
      *   The pre-filtered collection of available candidate placements.
      * @return
      *   [[Some]] candidate [[Placement]] minimizing the spatial distance, or
      *   [[None]] if the mission has no target destination or if no candidate
      *   is reachable.
      */
    override def selectFromAvailable(
        mission: Mission,
        available: Iterable[Placement]
    ): Option[Placement] =
      mission.currentDestination match
        case Some(target) =>
          available
            .flatMap(candidate =>
              navigator.distance(candidate.at, target).map(candidate -> _)
            )
            .minByOption(_._2)
            .map(_._1)
        case None => None
