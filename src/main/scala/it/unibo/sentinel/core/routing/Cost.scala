package it.unibo.sentinel.core.routing

/** Represents the weight that a [[Metric]] minimizes.
  */
opaque type Cost = Double

object Cost:
  /** @param value
    *   a non-negative value representing the cost.
    * @return
    *   a [[Cost]] built from the given value.
    * @throws IllegalArgumentException
    *   if the value is negative.
    */
  def apply(value: Double): Cost =
    require(value >= 0)
    value

  /** @return
    *   The zero cost.
    */
  val zero: Cost = Cost(0.0)

  /** @return
    *   The unit cost.
    */
  val unit: Cost = Cost(1.0)

  extension (cost: Cost)
    /** @return
      *   A [[Double]] representing the value of the [[Cost]].
      */
    def value: Double = cost

    /** @param other
      *   The other [[Cost]] to add.
      * @return
      *   The sum of this [[Cost]] and the other [[Cost]].
      */
    def +(other: Cost): Cost = cost + other

  given Ordering[Cost] = Ordering.Double.TotalOrdering
