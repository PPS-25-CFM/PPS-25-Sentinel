package it.unibo.sentinel.core.item

/** Represents a weight for an [[Item]].
  */
opaque type Weight = Double

object Weight:
  /** Weight constant */
  val zero: Weight = 0
  val max: Weight = 200.0
  val average: Weight = 50.0

  def apply(weight: Double): Weight = Math.max(weight, zero.value)

  /** @param weight
    *   the weight to decompose.
    * @return
    *   [[Some]] with the raw [[Double]] value.
    */
  def unapply(weight: Weight): Option[Double] = Some(weight)

  extension (weight: Weight)
    /** @return
      *   the [[Weight]] as a raw Double
      */
    def value: Double = weight

    /** @param other
      *   the weight to sum.
      * @return
      *   the sum of the weights.
      */
    def +(other: Weight): Weight = Weight(
      weight.value + other.value
    )

    /** @param other
      *   the weight to sum.
      * @return
      *   the difference of the weights.
      */
    def -(other: Weight): Weight = Weight(
      weight.value - other.value
    )

  /** Ordering by raw weight value, enables standard comparisons and sorting. */
  given Ordering[Weight] = Ordering.by(_.value)
