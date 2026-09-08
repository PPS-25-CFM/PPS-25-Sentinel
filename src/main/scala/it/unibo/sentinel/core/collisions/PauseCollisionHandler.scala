package it.unibo.sentinel.core.collisions

import it.unibo.sentinel.core.scenario.Placement
import it.unibo.sentinel.core.robot.RobotStatus
import it.unibo.sentinel.core.simulation.Tick

private object PauseCollisionHandler:
  def onWinner(placement: Placement): Seq[Action] =
    if placement.robot.status == RobotStatus.Waiting then
      Seq(Action.Unblock(placement.robot.id))
    else Seq.empty

  def onLoser(placement: Placement): Seq[Action] =
    if placement.robot.status == RobotStatus.Moving then
      Seq(Action.Block(placement.robot.id))
    else Seq.empty

private[collisions] final class PauseCollisionHandler
    extends BasicHandler(
      PauseCollisionHandler.onWinner,
      PauseCollisionHandler.onLoser
    ):

  override def resolveIndirectCollisions(
      winner: Placement,
      losers: Seq[Placement]
  ): Seq[Action] =
    val target = winner.intent.to
    val isStationary: Placement => Boolean = p => p.intent.from == p.intent.to
    if isStationary(winner) then losers.filterNot(isStationary).flatMap(onLoser)
    else if losers.exists(p => p.intent.from == target && isStationary(p)) then
      val movingLosers = losers.filterNot(isStationary)
      onLoser(winner) ++ movingLosers.flatMap(onLoser)
    else onWinner(winner) ++ losers.flatMap(onLoser)

  override def resolveDirectCollisions(
      winner: Placement,
      loser: Placement
  ): Seq[Action] =
    onLoser(winner) ++ onLoser(loser)

  override def cleanup(placements: Seq[Placement]): Seq[Action] =
    placements.flatMap { placement =>
      val canMove = CollisionChecker.canMove(placement, placements)
      val isReady = placement.robot.remaining == Tick.zero
      if !canMove && isReady && placement.robot.status == RobotStatus.Moving
      then onLoser(placement)
      else Seq.empty
    }
