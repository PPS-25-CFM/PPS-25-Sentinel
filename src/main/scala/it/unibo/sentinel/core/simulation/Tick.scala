package it.unibo.sentinel.core.simulation

/** Represents the unit of time in the simulation.
  */
opaque type Tick = Int

object Tick:
  /** @param value
    *   the value of the tick, must be non-negative.
    * @return
    *   a new tick with the given value.
    */
  def apply(value: Int): Tick =
    require(value >= 0)
    value

  extension (tick: Tick)
    /** Returns the value of the tick.
      */
    def value: Int = tick

    /** @return
      *   the previous tick, or 0 if the current tick is 0.
      */
    def previous: Tick = Math.max(tick - 1, 0)

    /** @return
      *   the next tick.
      */
    def next: Tick = tick + 1

  given Ordering[Tick] = Ordering.Int
  export scala.math.Ordering.Implicits.given
