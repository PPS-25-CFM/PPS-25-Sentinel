package it.unibo.sentinel.core.routing

import it.unibo.sentinel.core.warehouse.{Warehouse, Position}
import scala.annotation.tailrec
import scala.math.Ordering.Implicits.infixOrderingOps
import it.unibo.sentinel.core.warehouse

/** Represents the component that can compute [[Path]]s between positions in a
  * [[Warehouse]].
  */
trait Navigator:
  /** @return
    *   The [[Warehouse]] to navigate.
    */
  given warehouse: Warehouse

  /** @param from
    *   the starting [[Position]].
    * @param to
    *   the destination [[Position]].
    * @return
    *   An [[Option]] containing a [[Path]] between `from` and `to` if a path
    *   exists in the given [[Warehouse]].
    */
  def path(from: Position, to: Position): Option[Path]

  /** @param from
    *   the starting [[Position]].
    * @param to
    *   the destination [[Position]].
    * @return
    *   The distance between `from` and `to` if a [[Path]] exists in the given
    *   [[Warehouse]].
    */
  def distance(from: Position, to: Position): Option[Int] =
    path(from, to).map(_.positions.size)

object Navigator:
  /** @param metric
    *   the [[Metric]] to be used for computing distances.
    * @param w
    *   the [[Warehouse]] to navigate.
    * @return
    *   a [[Navigator]] that minimizes the given `metric`.
    */
  def apply(metric: Metric)(using w: Warehouse): Navigator =
    new Navigator:
      given warehouse: Warehouse = w
      override def path(from: Position, to: Position): Option[Path] =
        @tailrec
        def loop(
            fringe: Map[Position, Score],
            visited: Set[Position],
            parent: Map[Position, Position]
        ): Option[Path] =
          fringe.minByOption((_, d) => d) match
            case None            => None
            case Some((`to`, _)) => fromParent(parent)(from, to)
            case Some((curr, d)) =>
              val seen = visited + curr
              val relaxed = warehouse
                .traversableNeighbors(curr)
                .filterNot(seen)
                .flatMap(next => metric.cost(next).map(c => next -> (d + c)))
                .filterNot((next, c) => fringe.get(next).exists(_ <= c))
              loop(
                fringe - curr ++ relaxed,
                seen,
                parent ++ relaxed.map((next, _) => next -> curr)
              )
        loop(Map(from -> Score.zero), Set.empty, Map.empty)

      private def fromParent(
          parent: Map[Position, Position]
      )(from: Position, to: Position): Option[Path] =
        @tailrec
        def go(pos: Position, acc: Seq[Step]): Option[Seq[Step]] =
          if pos == from then Some(acc)
          else
            (parent.get(pos), warehouse.traversalCost(pos)) match
              case (Some(previous), Some(cost)) =>
                go(previous, Step(pos, cost) +: acc)
              case _ => None
        go(to, Seq.empty).map(legs => Path(legs*))
