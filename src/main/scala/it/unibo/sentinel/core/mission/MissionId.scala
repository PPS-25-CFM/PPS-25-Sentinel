package it.unibo.sentinel.core.mission

/** Represents a unique identifier for a mission.
  */
opaque type MissionId = String

object MissionId:
  /** Factory method to create a [[MissionId]] from a String.
    *
    * @param value
    *   The raw string representation of the mission identifier
    */
  def apply(value: String): MissionId = value

  extension (id: MissionId)
    /** @return
      *   The underlying String value from a [[MissionId]].
      */
    def value: String = id
