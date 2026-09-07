package it.unibo.sentinel.core.item

/** Represents a storable good that can be carried by a [[Robot]].
  *
  * @param weight
  *   the transport weight of the item.
  */
enum Item(val weight: Weight):

  case Computer extends Item(Weight(1.0))
  case Table extends Item(Weight(10.0))
  case Fridge extends Item(Weight(50.0))
  case Dishwasher extends Item(Weight(50.0))

object Item:

  /** Validation errors for [[Item]] creation. */
  enum Validation:
    /** The item has a negative weight.
      *
      * @param weight
      *   the invalid weight value.
      */
    case NegativeWeight(weight: Double)
