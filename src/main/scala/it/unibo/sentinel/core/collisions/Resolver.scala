package it.unibo.sentinel.core.collisions

import it.unibo.sentinel.core.robot.RobotId
import it.unibo.sentinel.core.warehouse.Position
import it.unibo.sentinel.core.robot.value
import scala.annotation.tailrec
import it.unibo.sentinel.core.scenario.Intent

private case class Context(
    movers: Seq[Intent],
    occupants: Map[Position, RobotId],
    stationaryCells: Set[Position]
)

private object Context:
  def from(intents: Seq[Intent]): Context =
    val ordered = intents.sortBy(_.robotId.value)
    val (movers, stationary) = ordered.partition(i => i.from != i.to)
    val occupants = ordered.map(i => i.from -> i.robotId).toMap
    Context(movers, occupants, stationary.map(_.from).toSet)

private final class Resolver(onYield: Intent => Action)
    extends CollisionHandler:

  override def resolveCollisions(
      intents: Seq[Intent]
  )(using selector: SelectionPolicy): Map[RobotId, Action] =
    val Context(movers, occupants, stationaryCells) = Context.from(intents)
    val cellLosers = chooseCellLosers(movers, stationaryCells)
    val cellWinners = movers.filterNot(i => cellLosers.contains(i.robotId))
    val swapLosers = chooseSwapLosers(cellWinners)
    val allLosers = cellLosers ++ swapLosers
    val winners = cellWinners.map(_.robotId).toSet -- swapLosers
    val allWinners = resolveChainDependencies(winners, movers, occupants)
    assignActions(movers, allLosers, allWinners)

  /** @param movers
    *   [[Intent]]s of robots attempting to change position.
    * @param stationaryCells
    *   occupied [[Position]]s of non-moving robots.
    * @param selector
    *   [[SelectionPolicy]] to resolve cell contentions.
    * @return
    *   `Set` of [[RobotId]]s that lost contention for a cell.
    */
  private def chooseCellLosers(
      movers: Seq[Intent],
      stationaryCells: Set[Position]
  )(using selector: SelectionPolicy): Set[RobotId] =
    movers
      .groupBy(_.to)
      .toSeq
      .sortBy((position, _) => (position.x, position.y))
      .flatMap { (targetCell, candidates) =>
        val chosenWinner =
          if stationaryCells.contains(targetCell) then None
          else selector.select(candidates)
        val ids = candidates.map(_.robotId)
        chosenWinner match
          case Some(winnerId) => ids.filterNot(_ == winnerId)
          case None           => ids
      }
      .toSet

  /** @param contenders
    *   remaining [[Intent]]s eligible to move.
    * @param selector
    *   [[SelectionPolicy]] to resolve swap conflicts.
    * @return
    *   `Set` of [[RobotId]]s that lost head-to-head swap contentions.
    */
  private def chooseSwapLosers(
      contenders: Seq[Intent]
  )(using selector: SelectionPolicy): Set[RobotId] =
    val byOrigin = contenders.map(i => i.from -> i).toMap
    val swapPairs = for
      first <- contenders
      second <- byOrigin.get(first.to)
      if isHeadToHeadSwap(first, second) && isCanonicalOrder(first, second)
    yield (first, second)
    swapPairs.flatMap { (p1, p2) =>
      val pair = Seq(p1, p2)
      val chosenWinner = selector.select(pair)
      pair.filterNot(i => chosenWinner.contains(i.robotId)).map(_.robotId)
    }.toSet

  private def isHeadToHeadSwap(i1: Intent, i2: Intent): Boolean =
    i2.to == i1.from

  private def isCanonicalOrder(i1: Intent, i2: Intent): Boolean =
    i1.robotId.value < i2.robotId.value

  /** @param winners
    *   `Set` of [[RobotId]]s currently winning their movement contention.
    * @param movers
    *   all movement [[Intent]]s.
    * @param occupants
    *   `Map` associating positions to occupying [[RobotId]]s.
    * @return
    *   `Set` of [[RobotId]]s capable of moving after resolving chain
    *   dependencies.
    */
  @tailrec
  private def resolveChainDependencies(
      winners: Set[RobotId],
      movers: Seq[Intent],
      occupants: Map[Position, RobotId]
  ): Set[RobotId] =
    val winningMovers = movers
      .filter { intent =>
        winners.contains(intent.robotId) &&
        occupants.get(intent.to).forall(winners.contains)
      }
      .map(_.robotId)
      .toSet
    if winningMovers == winners then winningMovers
    else resolveChainDependencies(winningMovers, movers, occupants)

  /** @param movers
    *   all movement [[Intent]]s.
    * @param losers
    *   `Set` of [[RobotId]]s that lost their contention.
    * @param winners
    *   `Set` of [[RobotId]]s clear to move.
    * @return
    *   `Map` assigning the target [[Action]] to each [[RobotId]].
    */
  private def assignActions(
      movers: Seq[Intent],
      losers: Set[RobotId],
      winners: Set[RobotId]
  ): Map[RobotId, Action] =
    movers.map { intent =>
      val id = intent.robotId
      val action =
        if losers.contains(id) then onYield(intent)
        else if winners.contains(id) then Action.Move
        else Action.Wait
      id -> action
    }.toMap
