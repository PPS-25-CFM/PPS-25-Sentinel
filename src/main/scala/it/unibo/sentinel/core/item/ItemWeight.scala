package it.unibo.sentinel.core.item

/** Represents a weight for an [[Item]].
  */
opaque type ItemWeight = Double

object ItemWeight:
  /** Zero weight constant */
  val Zero: ItemWeight = 0

  def apply(weight: Double): ItemWeight = Math.max(weight, Zero.value)

  /** @param weight
    *   the weight to decompose.
    * @return
    *   [[Some]] with the raw [[Double]] value.
    */
  def unapply(weight: ItemWeight): Option[Double] = Some(weight)

  extension (weight: ItemWeight)
    /** @return
      *   the [[ItemWeight]] as a raw Double
      */
    def value: Double = weight

    /** @param other
      *   the weight to sum.
      * @return
      *   the sum of the weights.
      */
    def +(other: ItemWeight): ItemWeight = ItemWeight(
      weight.value + other.value
    )

    /** @param other
      *   the weight to sum.
      * @return
      *   the difference of the weights.
      */
    def -(other: ItemWeight): ItemWeight = ItemWeight(
      weight.value - other.value
    )

  /** Ordering by raw weight value, enables standard comparisons and sorting. */
  given Ordering[ItemWeight] = Ordering.by(_.value)
