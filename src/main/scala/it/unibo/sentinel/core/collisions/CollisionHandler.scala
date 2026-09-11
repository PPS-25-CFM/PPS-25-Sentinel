package it.unibo.sentinel.core.collisions

import it.unibo.sentinel.core.robot.RobotId
import it.unibo.sentinel.core.routing.Path
import it.unibo.sentinel.core.routing.Navigator
import it.unibo.sentinel.core.scenario.Intent
import it.unibo.sentinel.core.mission.Action as MissionAction

/** Represents an action that a [[Robot]] must perform.
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

  /** @param intents
    *   the movement intents of the [[Robot]]s.
    * @param selector
    *   [[SelectionPolicy]] to use to resolve conflicts.
    * @return
    *   a `Map` association of [[RobotId]] to the assigned [[Action]].
    */
  def resolveCollisions(intents: Seq[Intent])(using
      selector: SelectionPolicy
  ): Map[RobotId, Action]

object CollisionHandler:

  /** [[CollisionHandler]] that makes the losers of collision disputes wait for
    * the cell to become unoccupied.
    */
  def pause(): CollisionHandler =
    new Resolver(_ => Action.Wait)

  /** [[CollisionHandler]] that makes the losers of collision disputes choose
    * another path towards their goal. If no path exists, they wait for the cell
    * to become unoccupied.
    */
  def reroute()(using navigator: Navigator): CollisionHandler =
    Resolver { intent =>
      val targetNodes = for
        mission <- intent.mission
        action <- mission.currentAction
      yield action match
        case MissionAction.PickUp(_, to) =>
          navigator.warehouse.neighbors(to).toSet
        case MissionAction.Move(to)    => Set(to)
        case MissionAction.Drop(_, to) => Set(to)
      val alternativePath = targetNodes.flatMap { targets =>
        navigator.path(intent.from, targets, avoiding = Set(intent.to))
      }
      alternativePath match
        case Some(path) => Action.Reroute(path)
        case None       => Action.Wait
    }
