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

  /** @return
    *   a [[Tick]] representing the zero.
    */
  val zero: Tick = Tick(0)

  /** @return
    *   a [[Tick]] representing the unit of time.
    */
  val unit: Tick = Tick(1)

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

    /** @param n
      *   the amount to subtract.
      * @return
      *   a new [[Tick]] minus "n", or 0 if the current tick is 0.
      */
    def -(n: Int): Tick = Math.max(tick - n, 0)

  given Ordering[Tick] = Ordering.Int
  export scala.math.Ordering.Implicits.given
