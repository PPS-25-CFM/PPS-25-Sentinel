package it.unibo.sentinel.core.mission

opaque type MissionId = String

object MissionId:
  def apply(value: String): MissionId = value

  extension (id: MissionId) def value: String = id
