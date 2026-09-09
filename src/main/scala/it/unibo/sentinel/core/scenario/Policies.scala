package it.unibo.sentinel.core.scenario

import it.unibo.sentinel.core.routing.{Navigator, Metric}
import it.unibo.sentinel.core.warehouse.Warehouse
import it.unibo.sentinel.core.assignment.Selector
import it.unibo.sentinel.core.collisions.SelectionPolicy
import it.unibo.sentinel.core.collisions.CollisionHandler
import it.unibo.sentinel.core.mission.Mission

/** Represents the policies that govern the behavior of the simulation.
  */
object Policies:
  /** Routing policies, i.e. how routes are determined.
    */
  enum Routing:
    /** Routes are determined based on distance.
      */
    case Distance

    /** Routes are determined based on time.
      */
    case Time

    /** @return
      *   the [[Navigator]] for the given [[Routing]] policy.
      */
    def apply()(using Warehouse): Navigator = this match
      case Distance => Navigator(Metric.Hops)
      case Time     => Navigator(Metric.Time)

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

  enum CollisionSelection:

    case Random
    case Deadline
    case Priority

    def apply()(using missionSupplier: => Seq[Mission]): SelectionPolicy =
      this match
        case Random   => SelectionPolicy.random()
        case Deadline => SelectionPolicy.closestDeadline()
        case Priority => SelectionPolicy.highestPriority()

  enum CollisionAvoidance:

    case Wait
    case Reroute

    def apply()(using navigator: Navigator): CollisionHandler = this match
      case Wait    => CollisionHandler.pause()
      case Reroute => CollisionHandler.reroute()
