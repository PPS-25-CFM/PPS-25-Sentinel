package it.unibo.sentinel.core.mission

import it.unibo.sentinel.core.warehouse.Position

enum Task:
  case Done
  case Fail
  case Move(at: Position)

  def isDone: Boolean = this == Task.Done
  def isFail: Boolean = this == Task.Fail

  def where: Option[Position] = this match
    case Move(at) => Some(at)
    case _        => None
