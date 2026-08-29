package it.unibo.sentinel.core.routing

import it.unibo.sentinel.core.warehouse.Position
import it.unibo.sentinel.core.simulation.Tick

/** @param to
  * @param cost
  */
case class Step(to: Position, cost: Tick)

/** A [[Path]] that a robot follows.
  */
opaque type Path = Seq[Step]

object Path:

  /** @return
    *   an empty [[Path]]
    */
  def empty: Path = Seq.empty

  /** @param steps
    *   the [[Step]]s that compose the [[Path]].
    * @return
    *   a [[Path]] made of the given [[Step]]s
    */
  def apply(steps: Step*): Path = steps.toSeq

  extension (path: Path)

    /** @return
      *   the [[Position]]s contained in the [[Path]]
      */
    def positions: Seq[Position] = path.map(_.to)

    /** @return
      *   the remaining time to cross the next [[Position]] in the [[Path]].
      */
    def remaining: Tick = path.headOption.map(_.cost).getOrElse(Tick.zero)

    /** @return
      *   the [[Path]] with the first [[Step]] decreased by one tick.
      */
    def ticked: Path = path match
      case step +: rest => step.copy(cost = step.cost.previous) +: rest
      case _            => path

    /** @return
      *   the [[Path]] with the first [[Step]] removed if its cost is zero,
      *   otherwise the same [[Path]].
      */
    def advanced: Path = path match
      case step +: rest if step.cost == Tick.zero => rest
      case _                                      => path
