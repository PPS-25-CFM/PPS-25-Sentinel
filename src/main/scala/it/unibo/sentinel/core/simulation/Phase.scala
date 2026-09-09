package it.unibo.sentinel.core.simulation

import it.unibo.sentinel.core.assignment.Selector
import it.unibo.sentinel.core.routing.Navigator
import it.unibo.sentinel.core.robot.RobotStatus.*
import it.unibo.sentinel.core.collisions.CollisionHandler
import it.unibo.sentinel.core.collisions.SelectionPolicy
import it.unibo.sentinel.core.mission.Action
import it.unibo.sentinel.core.warehouse.{Position, Warehouse}

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
      action <- current.currentAction
      destinations = action match
        case Action.Move(to)      => Set(to)
        case Action.Drop(_, at)   => Set(at)
        case Action.PickUp(_, at) =>
          world.warehouse.interactionPoints(at).toSet
      if destinations.nonEmpty
      path <- navigator.path(spot.at, destinations)
      routed <- world.route(robot.id, path)
    yield routed

  def collisionHandling(using
      handler: CollisionHandler,
      selector: SelectionPolicy
  ): Phase = world =>
    val events = for
      (robotId, action) <- handler.resolveCollisions(world.placements)
      event <- world.execute(robotId, action)
    yield event
    events.toSeq

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
      mid <- robot.mission.toSeq
      mission <- world.mission(mid).toSeq
      action <- mission.currentAction.toSeq
      if isSatisfied(world.warehouse, spot.at, action)
      performed <- world.perform(robot.id)
    yield performed

  private def isSatisfied(
      warehouse: Warehouse,
      at: Position,
      action: Action
  ): Boolean = action match
    case Action.Move(to)         => at == to
    case Action.Drop(_, bay)     => at == bay
    case Action.PickUp(_, shelf) =>
      warehouse.interactionPoints(shelf).contains(at)

  def expiring: Phase = _.tick()

  def all(using
      Selector,
      Navigator,
      CollisionHandler,
      SelectionPolicy
  ): Seq[Phase] =
    Seq(expiring, assigning, routing, collisionHandling, moving, performing)
