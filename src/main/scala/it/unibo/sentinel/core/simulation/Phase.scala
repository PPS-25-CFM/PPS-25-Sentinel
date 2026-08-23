package it.unibo.sentinel.core.simulation

import it.unibo.sentinel.core.assignment.Selector
import it.unibo.sentinel.core.routing.Navigator
import it.unibo.sentinel.core.robot.RobotStatus.*

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
      destination <- current.currentDestination
      path <- navigator.path(spot.at, destination)
      routed <- world.route(robot.id, path)
    yield routed

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
      target <- mission.currentDestination
      if spot.at == target
      performed <- world.perform(robot.id)
    yield performed

  def expiring: Phase = _.tick()

  def all(using Selector, Navigator): Seq[Phase] =
    Seq(expiring, assigning, routing, moving, performing)
