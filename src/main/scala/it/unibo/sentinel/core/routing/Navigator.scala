package it.unibo.sentinel.core.routing

import it.unibo.sentinel.core.warehouse.{Warehouse, Position}

/** Abstracts the metric that a [[Navigator]] uses in order to compute a path
  * between two positions.
  */
trait Metric

object Metric:
  /** Metric that assigns a unit cost to every traversed position.
    */
  object Hops extends Metric

/** A [[Path]] is a type alias for a [[Seq]] of [[Position]], where each
  * [[Position]] is a step in the [[Path]].
  */
type Path = Seq[Position]

/** Represents the component that can compute paths and distances between
  * positions in a [[Warehouse]].
  */
trait Navigator:
  /** @return
    *   The [[Warehouse]] to navigate.
    */
  given warehouse: Warehouse

  /** Compute a [[Path]] between [[from]] and [[to]] if a path exists in the
    * given [[Warehouse]].
    */
  def path(from: Position, to: Position): Option[Path]

object Navigator:
  /** @param metric
    *   the metric to be used for computing distances.
    * @return
    *   a [[Navigator]] that uses the given [[metric]].
    */
  def apply(metric: Metric)(using w: Warehouse): Navigator =
    new Navigator:
      given warehouse: Warehouse = w
      val _ = metric
      override def path(from: Position, to: Position): Option[Path] =
        None
