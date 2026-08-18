package it.unibo.sentinel.core.mission

import it.unibo.sentinel.core.warehouse.Position


enum Task:
  case Act(step: Step)

  def destination: Position = this match
    case Act(step) => step.target