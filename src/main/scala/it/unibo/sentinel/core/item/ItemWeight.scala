package it.unibo.sentinel.core.item

/** Represents a non-negative weight for an [[Item]] (strictly >= 0).
  */
opaque type ItemWeight = Double

object ItemWeight:
  /** Zero weight constant */
  val Zero: ItemWeight = 0

  /** Constructor that ensures weight is always >= 0.
    */
  def apply(weight: Double): ItemWeight = Math.max(weight, Zero.value)

  def unapply(weight: ItemWeight): Option[Double] = Some(weight)

  /** Explicit extension methods for ItemWeight operations
    */
  extension (weight: ItemWeight)
    /** @return
      *   the [[ItemWeight]] as a raw Double
      */
    def value: Double = weight

    /** Adds another [[ItemWeight]].
      */
    def +(other: ItemWeight): ItemWeight = ItemWeight(
      weight.value + other.value
    )

    /** Subtracts another [[ItemWeight]], capped at 0.
      */
    def -(other: ItemWeight): ItemWeight = ItemWeight(
      weight.value - other.value
    )

  // Enables standard comparisons and sorting
  given Ordering[ItemWeight] = Ordering.by(_.value)
