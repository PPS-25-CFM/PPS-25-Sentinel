package it.unibo.sentinel.core.selection

import it.unibo.sentinel.core.mission.Mission
import it.unibo.sentinel.core.robot.Robot
import it.unibo.sentinel.core.warehouse.Position

final case class Placement(
  robot: Robot,
  at: Position
)

trait Selector:

  def choose(
    mission: Mission,
    among:Iterable[Placement]
  ): Option[Placement] =
    val available = among.filter(_._1.canAccept)
    if available.isEmpty then None
    else selectFromAvailable(mission, available)

  def selectFromAvailable(
    mission: Mission,
    available: Iterable[Placement]
  ): Option[Placement] = ???

object Selector:

  import it.unibo.sentinel.core.routing.Navigator

  case class Nearest(navigator: Navigator) extends Selector:
      override def selectFromAvailable(
          mission: Mission,
          available: Iterable[Placement]
      ): Option[Placement] =
        mission.currentDestination match
          case None         => available.headOption
          case Some(target) =>
            available
              .flatMap(candidate =>
                navigator.distance(candidate.at, target).map(candidate -> _)
              )
              .minByOption(_._2)
              .map(_._1)
