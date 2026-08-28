package it.unibo.sentinel.core.routing

/** Represents the weight that a [[Metric]] minimizes.
  */
opaque type Score = Double

object Score:
  /** @param value
    *   a non-negative value representing the cost.
    * @return
    *   a [[Score]] built from the given value.
    * @throws IllegalArgumentException
    *   if the value is negative.
    */
  def apply(value: Double): Score =
    require(value >= 0)
    value

  /** @return
    *   zero [[Score]].
    */
  val zero: Score = Score(0.0)

  extension (score: Score)
    /** @return
      *   A [[Double]] representing the [[Score]].
      */
    def value: Double = score

    /** @param other
      *   a [[Score]].
      * @return
      *   the sum of `score` and `other`.
      */
    def +(other: Score): Score = score + other

  given Ordering[Score] = Ordering.Double.TotalOrdering
