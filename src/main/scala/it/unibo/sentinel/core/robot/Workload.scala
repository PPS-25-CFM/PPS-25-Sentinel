package it.unibo.sentinel.core.robot

/** The Workload level of a robot
  */
opaque type Workload = Int

object Workload:
  /** @param value
    *   the current level, must be non-negative.
    * @return
    *   a new workload with the given value.
    */
  def apply(value: Int): Workload =
    require(value >= 0, "workload must be non-negative!")
    value

  /** Workload constants
    */
  val zero: Workload = Workload(0)

  extension (workload: Workload)
    /** @return
      *   the workload as a raw Int.
      */
    def value: Int = workload

  given Ordering[Workload] = Ordering.Int
