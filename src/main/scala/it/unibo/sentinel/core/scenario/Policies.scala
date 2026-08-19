package it.unibo.sentinel.core.scenario

import it.unibo.sentinel.core.routing.{Navigator, Metric}
import it.unibo.sentinel.core.warehouse.Warehouse
import it.unibo.sentinel.core.assignment.Selector

/** Represents the policies that govern the behavior of the simulation.
  */
object Policies:
  /** Routing policies, i.e. how routes are determined.
    */
  enum Routing:
    /** Routes are determined based on distance.
      */
    case Distance

    /** @return
      *   the [[Navigator]] for the given [[Routing]] policy.
      */
    def apply()(using Warehouse): Navigator = this match
      case Distance => Navigator(Metric.Hops)

  /** Assignment policies, i.e. how mission are assigned.
    */
  enum Assignment:
    /** Assignment based on distance from target.
      */
    case Nearest

    /** @return
      *   the [[Selector]] for the given [[Assignment]] policy.
      */
    def apply()(using nav: Navigator): Selector = this match
      case Nearest => Selector.Nearest(nav)
