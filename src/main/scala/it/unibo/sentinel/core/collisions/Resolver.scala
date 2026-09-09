package it.unibo.sentinel.core.collisions

import it.unibo.sentinel.core.scenario.Placement
import it.unibo.sentinel.core.robot.RobotId
import it.unibo.sentinel.core.warehouse.Position
import it.unibo.sentinel.core.robot.value
import scala.annotation.tailrec

private case class Context(
    movers: Seq[Placement],
    occupants: Map[Position, RobotId],
    stationaryCells: Set[Position]
)

private object Context:
  def from(placements: Seq[Placement]): Context =
    val ordered = placements.sortBy(_.robot.id.value)
    val (movers, stationary) =
      ordered.partition(p => p.intent.from != p.intent.to)
    Context(
      movers = movers,
      occupants = ordered.map(p => p.intent.from -> p.intent.robotId).toMap,
      stationaryCells = stationary.map(_.intent.from).toSet
    )

private final class Resolver(onYield: Placement => Action)
    extends CollisionHandler:

  override def resolveCollisions(
      placements: Seq[Placement]
  )(using selector: SelectionPolicy): Map[RobotId, Action] =
    val Context(movers, occupants, stationaryCells) = Context.from(placements)
    val cellLosers = chooseCellLosers(movers, stationaryCells)
    val cellWinners = movers.filterNot(p => cellLosers.contains(p.robot.id))
    val swapLosers = chooseSwapLosers(cellWinners)
    val allLosers = cellLosers ++ swapLosers
    val winners = cellWinners.map(_.robot.id).toSet -- swapLosers
    val allWinners = resolveChainDependencies(winners, movers, occupants)
    assignActions(movers, allLosers, allWinners)

  /** @param movers
    *   [[Placement]]s that want to move from a cell to another.
    * @param stationaryCells
    *   occupied [[Position]]s.
    * @param selector
    *   [[SelectionPolicy]] to determine the winner and the losers of the
    *   collisions.
    * @return
    *   a `Set` of [[RobotId]]s of the [[Robot]]s that lost the contention of a
    *   cell.
    */
  private def chooseCellLosers(
      movers: Seq[Placement],
      stationaryCells: Set[Position]
  )(using selector: SelectionPolicy): Set[RobotId] =
    movers
      .groupBy(_.intent.to)
      .toSeq
      .sortBy((position, _) => (position.x, position.y))
      .flatMap { (targetCell, candidates) =>
        val chosenWinner =
          if stationaryCells.contains(targetCell) then None
          else selector.select(candidates.map(_.robot))
        val ids = candidates.map(_.robot.id)
        chosenWinner match
          case Some(winnerId) => ids.filterNot(_ == winnerId)
          case None           => ids
      }
      .toSet

  /** @param contenders
    *   remaining [[Placement]]s that can move
    * @param selector
    *   [[SelectionPolicy]] to determine the winner and the losers of the
    *   collisions.
    * @return
    *   a `Set` of [[RobotId]]s of the [[Robot]]s that lost in the attempted
    *   swap.
    */
  private def chooseSwapLosers(
      contenders: Seq[Placement]
  )(using selector: SelectionPolicy): Set[RobotId] =
    val byOrigin = contenders.map(p => p.intent.from -> p).toMap
    val swapPairs = for
      first <- contenders
      second <- byOrigin.get(first.intent.to)
      if isHeadToHeadSwap(first, second) && isCanonicalOrder(first, second)
    yield (first, second)
    swapPairs.flatMap { (p1, p2) =>
      val pair = Seq(p1, p2)
      val chosenWinner = selector.select(pair.map(_.robot))
      pair.filterNot(p => chosenWinner.contains(p.robot.id)).map(_.robot.id)
    }.toSet

  private def isHeadToHeadSwap(p1: Placement, p2: Placement): Boolean =
    p2.intent.to == p1.intent.from

  private def isCanonicalOrder(p1: Placement, p2: Placement): Boolean =
    p1.robot.id.value < p2.robot.id.value

  /** @param winners
    *   `Set` of [[RobotId]]s of the [[Robot]]s that won the collisions'
    *   contests.
    * @param movers
    *   list of [[Placement]]s that want to move.
    * @param occupants
    *   `Map` that indicates which [[Position]]s are occupied by which
    *   [[Placement]].
    * @return
    *   a `Set` of [[RobotId]]s of the [[Robot]]s that won the remaining
    *   collisions.
    */
  @tailrec
  private def resolveChainDependencies(
      winners: Set[RobotId],
      movers: Seq[Placement],
      occupants: Map[Position, RobotId]
  ): Set[RobotId] =
    val winningMovers = movers
      .filter { p =>
        winners.contains(p.robot.id) &&
        occupants.get(p.intent.to).forall(winners.contains)
      }
      .map(_.robot.id)
      .toSet
    if winningMovers == winners then winningMovers
    else resolveChainDependencies(winningMovers, movers, occupants)

  /** @param movers
    *   list of [[Placement]]s that want to move.
    * @param losers
    *   losers of the collisions.
    * @param winners
    *   winners of the collisions.
    * @return
    *   a `Map` that indicates which [[Robot]] (represented by its id) has to do
    *   what.
    */
  private def assignActions(
      movers: Seq[Placement],
      losers: Set[RobotId],
      winners: Set[RobotId]
  ): Map[RobotId, Action] =
    movers.map { placement =>
      val id = placement.robot.id
      val action =
        if losers.contains(id) then onYield(placement)
        else if winners.contains(id) then Action.Move
        else Action.Wait
      id -> action
    }.toMap
