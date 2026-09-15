package it.unibo.sentinel.core.mission

opaque type Priority = Int

object Priority:
  /** Priority Low Constant */
  val lowest: Priority = 1

  /** Priority Normal Constant */
  val normal: Priority = 5

  /** Priority High Constant */
  val highest: Priority = 10

  /** Creates a new Priority instance if the value is valid */
  def apply(value: Int): Priority =
    require(
      lowest <= value && value <= highest,
      s"priority must be within $lowest and $highest!"
    )
    value

  /** Safely creates a Priority, returning [[None]] if out of range */
  def from(value: Int): Option[Priority] =
    Option.when(lowest <= value && value <= highest)(value)

  given Ordering[Priority] = Ordering.Int

  extension (priority: Priority)
    /** Returns the integer value of the priority */
    def value: Int = priority
