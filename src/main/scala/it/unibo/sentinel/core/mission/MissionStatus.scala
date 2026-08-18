package it.unibo.sentinel.core.mission

/** Represents the possible lifecycle states of a [[Mission]] within the
  * Sentinel system.
  */
enum MissionStatus:
  /** The mission is created and waiting to be assigned to a robot. */
  case Pending

  /** The mission has been assigned to a robot carrier and is currently active.
    */
  case Assigned

  /** Terminal state indicating that the mission task was successfully
    * accomplished.
    */
  case Completed

  /** Terminal state indicating that the mission failed (e.g., due to task
    * failure or time expiration).
    */
  case Failed
