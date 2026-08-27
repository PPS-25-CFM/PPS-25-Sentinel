package it.unibo.sentinel.core.routing

import it.unibo.sentinel.core.warehouse.{Warehouse, Position}

/** Abstracts the metric that a [[Navigator]] uses in order to compute a path
  * between two positions.
  */
trait Metric:
  /** @param to
    *   the [[Position]] to move to.
    * @return
    *   an [[Option]] containing the [[Score]] for moving to the given
    *   [[Position]], if it is traversable.
    */
  def cost(to: Position)(using warehouse: Warehouse): Option[Score]

object Metric:
  /** Assigns a unit [[Score]] to every traversed position.
    */
  object Hops extends Metric:
    override def cost(to: Position)(using warehouse: Warehouse): Option[Score] =
      Option.when(warehouse.isTraversable(to))(Score(1))

  /** Assigns a [[Score]] to every traversed position based on its traversal
    * cost.
    */
  object Time extends Metric:
    override def cost(to: Position)(using warehouse: Warehouse): Option[Score] =
      for tick <- warehouse.traversalCost(to) yield Score(tick.value)
