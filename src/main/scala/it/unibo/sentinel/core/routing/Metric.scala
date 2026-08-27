package it.unibo.sentinel.core.routing

import it.unibo.sentinel.core.warehouse.{Warehouse, Position}

/** Abstracts the metric that a [[Navigator]] uses in order to compute a path
  * between two positions.
  */
trait Metric:
  /** @param to
    *   the position to move to.
    * @return
    *   the cost for moving in [[to]] based on the given [[Metric]].
    */
  def cost(to: Position)(using warehouse: Warehouse): Int

object Metric:
  /** Metric that assigns a unit cost to every traversed position.
    */
  object Hops extends Metric:
    override def cost(to: Position)(using warehouse: Warehouse): Int = 1

  /** Metric that assigns a cost to every traversed position based on the
    * traversal cost of the position
    */
  object Time extends Metric:
    override def cost(to: Position)(using warehouse: Warehouse): Int =
      warehouse.traversalCost(to).map(_.value).getOrElse(Int.MaxValue)
