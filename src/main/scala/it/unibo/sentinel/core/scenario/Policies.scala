package it.unibo.sentinel.core.scenario

import it.unibo.sentinel.core.routing.{Navigator, Metric}
import it.unibo.sentinel.core.warehouse.Warehouse
import it.unibo.sentinel.core.assignment.Selector
import it.unibo.sentinel.core.collisions.SelectionPolicy
import it.unibo.sentinel.core.collisions.CollisionHandler
import it.unibo.sentinel.core.mission.Mission
import scala.util.Random

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

    /** Round-robin assignment cycling through candidates.
      */
    case Cycle

    /** Random assignment.
      */
    case Random

    /** Assignment to the robot with the fewest assigned missions.
      */
    case LeastWorkload

    /** @param rng
      *   the random generator governing random choices.
      * @return
      *   the [[Selector]] for the given [[Assignment]] policy.
      */
    def apply(rng: Random)(using nav: Navigator): Selector = this match
      case Nearest       => Selector.Nearest(nav)
      case Cycle         => Selector.CycleSelector()
      case Random        => Selector.RandomSelector(rng)
      case LeastWorkload => Selector.LeastWorkload()

  enum CollisionSelection:

    case Random
    case Deadline
    case Priority

    /** @param rng
      *   the random generator governing random choices.
      * @return
      *   the [[SelectionPolicy]] for the given policy.
      */
    def apply(rng: Random)(using
        missionSupplier: => Seq[Mission]
    ): SelectionPolicy =
      this match
        case Random   => SelectionPolicy.random(rng)
        case Deadline => SelectionPolicy.closestDeadline()
        case Priority => SelectionPolicy.highestPriority()

  enum CollisionAvoidance:

    case Wait
    case Reroute

    def apply()(using navigator: Navigator): CollisionHandler = this match
      case Wait    => CollisionHandler.pause()
      case Reroute => CollisionHandler.reroute()
