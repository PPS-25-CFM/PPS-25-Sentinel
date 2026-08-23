package it.unibo.sentinel.core.simulation

import it.unibo.sentinel.core.assignment.Selector

private[core] type Phase = Environment => Seq[Event]

private[core] object Phase:

  def assigning(using selector: Selector): Phase = world =>
    for
      mission <- world.pendingMissions
      spot <- selector.choose(mission, world.placements)
      chosen = spot.robot
      assigned <- world.assign(chosen.id, mission.id)
    yield assigned
