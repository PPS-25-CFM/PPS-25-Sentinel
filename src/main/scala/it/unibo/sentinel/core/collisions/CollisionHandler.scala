package it.unibo.sentinel.core.collisions

import it.unibo.sentinel.core.scenario.Placement
import it.unibo.sentinel.core.robot.RobotId
import it.unibo.sentinel.core.routing.Path
import it.unibo.sentinel.core.routing.Navigator

/** Represents an action that a [[Robot]] must to perform.
  */
enum Action:
  /** The [[Robot]] must move.
    */
  case Move

  /** The [[Robot]] must wait.
    */
  case Wait

  /** The [[Robot]] must follow a new [[Path]].
    *
    * @param path
    *   new [[Path]] to follow.
    */
  case Reroute(path: Path)

/** Defines how to handle collisions between [[Robot]]s
  */
trait CollisionHandler:

  /** @param placements
    *   the placements that may collide.
    * @param selector
    *   [[SelectionPolicy]] to determine who wins and who loses on the
    *   conflicts.
    * @return
    *   a `Map` of [[RobotId]] and [[Action]] to indicate which [[Robot]] has to
    *   do what.
    */
  def resolveCollisions(placements: Seq[Placement])(using
      selector: SelectionPolicy
  ): Map[RobotId, Action]

object CollisionHandler:

  /** [[CollisionHandler]] that makes the losers of the collisions disputes wait
    * for the cell to become unoccupied.
    */
  def pause(): CollisionHandler =
    new Resolver(_ => Action.Wait)

  /** [[CollisionHandler]] that makes the losers of the collisions disputes
    * choose another path towards their goal. If no path exists, they wait for
    * the cell to become unoccupied.
    */
  def reroute()(using navigator: Navigator): CollisionHandler =
    new Resolver(placement =>
      val alternative = for
        currentPath <- placement.robot.path
        destination <- currentPath.destination
        path <- navigator.path(
          placement.intent.from,
          destination,
          avoiding = Set(placement.intent.to)
        )
      yield path
      alternative match
        case Some(p) => Action.Reroute(p)
        case None    => Action.Wait
    )
