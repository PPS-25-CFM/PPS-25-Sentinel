package it.unibo.sentinel.core.routing

/** Represents the weight that a [[Metric]] minimizes.
  */
opaque type Cost = Double

object Cost:
  /** @param value
    * @return
    */
  def apply(value: Double): Cost =
    require(value >= 0)
    value

  /** @return
    */
  val zero: Cost = Cost(0.0)

  /** @return
    */
  val unit: Cost = Cost(1.0)

  extension (cost: Cost)
    /** @return
      */
    def value: Double = cost

    /** @param other
      * @return
      */
    def +(other: Cost): Cost = cost + other

    /** @param weight
      * @return
      */
    def *(weight: Double): Cost =
      require(weight >= 0)
      Cost(cost * weight)

  given Ordering[Cost] = Ordering.Double.TotalOrdering
