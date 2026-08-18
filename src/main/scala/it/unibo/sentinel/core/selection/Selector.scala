package it.unibo.sentinel.core.selection

import it.unibo.sentinel.core.mission.Mission
import it.unibo.sentinel.core.robot.Robot
import it.unibo.sentinel.core.warehouse.Position

object Selector:

  def choose(
    mission: Mission,
    among:Iterable[(Robot, Position)]
  ): Option[(Robot, Position)] =
    val available = among.filter(_._1.canAccept)
    if available.isEmpty then None
    else selectFromAvailable(mission, available)

  def selectFromAvailable(
    mission: Mission,
    available: Iterable[(Robot, Position)]
  ): Option[(Robot, Position)] = ???
