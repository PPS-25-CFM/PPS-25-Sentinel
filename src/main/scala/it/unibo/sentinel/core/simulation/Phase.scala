package it.unibo.sentinel.core.simulation

import it.unibo.sentinel.core.assignment.Selector
import it.unibo.sentinel.core.routing.Navigator
import it.unibo.sentinel.core.robot.RobotStatus.*
import it.unibo.sentinel.core.collisions.CollisionHandler
import it.unibo.sentinel.core.collisions.CollisionChecker
import it.unibo.sentinel.core.collisions.SelectionPolicy

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
    val intents = world.placements.map(_.intent)
    val collisions = CollisionChecker.checkCollisions(intents)
    val events = for
      group <- collisions
      colliding = world.placements.filter(r => group.contains(r.robot.id))
    yield handler.resolveCollisions(colliding)
    events.flatMap(identity)

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
