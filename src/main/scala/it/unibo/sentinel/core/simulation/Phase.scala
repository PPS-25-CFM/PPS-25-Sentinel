package it.unibo.sentinel.core.simulation

import it.unibo.sentinel.core.assignment.Selector
import it.unibo.sentinel.core.routing.Navigator
import it.unibo.sentinel.core.robot.RobotStatus.*
import it.unibo.sentinel.core.collisions.CollisionHandler
import it.unibo.sentinel.core.collisions.SelectionPolicy
import it.unibo.sentinel.core.collisions.CollisionChecker
import it.unibo.sentinel.core.scenario.Placement

private[core] type Phase = Environment => Seq[Event]

private[core] object Phase:

  def assigning(using selector: Selector): Phase = world =>
    for
      mission <- world.pendingMissions
      spot <- selector.choose(mission, world.placements)
      chosen = spot.robot
      assigned <- world.assign(chosen.id, mission.id)
    yield assigned

  def routing(using navigator: Navigator): Phase = world =>
    for
      spot <- world.standing(Ready)
      robot = spot.robot
      current <-
        for
          mid <- robot.mission;
          mission <- world.mission(mid)
        yield mission
      destination <- current.currentTarget
      path <- navigator.path(spot.at, destination)
      routed <- world.route(robot.id, path)
    yield routed

  def collisionHandling(using
      handler: CollisionHandler,
      selector: SelectionPolicy
  ): Phase = world =>
    val rawIndirectEvents = for
      (winner, losers) <- getIndirectWinnerLosers(world)
      action           <- handler.resolveIndirectCollisions(winner, losers)
      event            <- world.execute(action)
    yield event
    val rawDirectEvents = for
      (winner, loser) <- getDirectWinnerLoser(world)
      action          <- handler.resolveDirectCollisions(winner, loser)
      event           <- world.execute(action)
    yield event
    val rawCleanupEvents = for
      action <- handler.cleanup(world.placements)
      event  <- world.execute(action)
    yield event
    cancelOpposites(rawIndirectEvents ++ rawDirectEvents ++ rawCleanupEvents)

  private def cancelOpposites(events: Seq[Event]): Seq[Event] =
    events.foldLeft(Vector.empty[Event]) { (acc, event) =>
      acc.indexWhere(_.isOppositeOf(event)) match
        case -1 => acc :+ event
        case idx => acc.patch(idx, Nil, 1)
    }

  private def getIndirectWinnerLosers(
      world: Environment
  )(using selector: SelectionPolicy): Seq[(Placement, Seq[Placement])] =
    val intents = world.placements.map(_.intent)
    for
      collision <- CollisionChecker.indirectCollisions(intents)
      robots = collision.robots.flatMap(world.robot)
      winnerId <- selector.select(robots)
      winner <- world.placement(winnerId)
      losers = robots.filterNot(_.id == winnerId)
      loserPlacements = losers.flatMap(r => world.placement(r.id))
    yield (winner, loserPlacements)

  private def getDirectWinnerLoser(
      world: Environment
  )(using selector: SelectionPolicy): Seq[(Placement, Placement)] =
    val intents = world.placements.map(_.intent)
    for
      collision <- CollisionChecker.directCollisions(intents)
      robots = Seq(collision.robot1, collision.robot2).flatMap(world.robot)
      winnerId <- selector.select(robots)
      winner <- world.placement(winnerId)
      loserId = if winnerId == collision.robot1 then collision.robot2 else collision.robot1
      loser <- world.placement(loserId)
    yield
      (winner, loser)

  def moving: Phase = world =>
    for
      spot <- world.standing(Moving)
      robot = spot.robot
      moved <- world.advance(robot.id)
    yield moved

  def performing: Phase = world =>
    for
      spot <- world.placements
      robot = spot.robot
      mid <- robot.mission
      mission <- world.mission(mid)
      target <- mission.currentTarget
      if spot.at == target
      performed <- world.perform(robot.id)
    yield performed

  def expiring: Phase = _.tick()

  def all(using
      Selector,
      Navigator,
      CollisionHandler,
      SelectionPolicy
  ): Seq[Phase] =
    Seq(expiring, assigning, routing, collisionHandling, moving, performing)
