package it.unibo.sentinel.core.collisions

import it.unibo.sentinel.core.scenario.Placement
import it.unibo.sentinel.core.robot.RobotId

enum Action(val id: RobotId):
  case Block(override val id: RobotId) extends Action(id)
  case Unblock(override val id: RobotId) extends Action(id)

/** Defines how to handle collisions between [[Robot]]s
  */
trait CollisionHandler:

  def resolveIndirectCollisions(
      winner: Placement,
      losers: Seq[Placement]
  ): Seq[Action]

  def resolveDirectCollisions(
      winner: Placement,
      loser: Placement
  ): Seq[Action]

  def cleanup(placements: Seq[Placement]): Seq[Action]

private abstract class BasicHandler(
    protected val onWinner: Placement => Seq[Action],
    protected val onLoser: Placement => Seq[Action]
) extends CollisionHandler:

  override def resolveIndirectCollisions(
      winner: Placement,
      losers: Seq[Placement]
  ): Seq[Action] =
    onWinner(winner) ++ losers.flatMap(onLoser)

  override def resolveDirectCollisions(
      winner: Placement,
      loser: Placement
  ): Seq[Action] =
    onWinner(winner) ++ onLoser(loser)

object CollisionHandler:

  /** Handler based on pausing [[Robot]]s
    *
    * @param selectionPolicy
    *   policy used to select the [[Robot]](s) that can move
    */
  def pausing(): CollisionHandler = new PauseCollisionHandler()
