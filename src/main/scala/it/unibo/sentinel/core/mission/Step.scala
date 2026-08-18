package it.unibo.sentinel.core.mission

import it.unibo.sentinel.core.warehouse.Position

trait Targeted:
  def target: Position

/** A single atomic physical operation required by a mission.
  */
enum Step extends Targeted:
  case Goto(target: Position)
