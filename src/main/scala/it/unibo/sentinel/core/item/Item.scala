package it.unibo.sentinel.core.item

/** Represents a storable good that can be carried by a [[Robot]].
  *
  * @param weight
  *   the transport weight of the item.
  */
enum Item(val weight: ItemWeight):

  case Computer extends Item(ItemWeight(1.0))
  case Table extends Item(ItemWeight(10.0))
  case Fridge extends Item(ItemWeight(50.0))
  case Dishwasher extends Item(ItemWeight(50.0))

object Item:

  /** Validation errors for [[Item]] creation. */
  enum Validation:
    /** The item has a negative weight.
      *
      * @param weight
      *   the invalid weight value.
      */
    case NegativeWeight(weight: Double)

  /** @return the maximum [[ItemWeight]] among all [[Item]] values. */
  def highestWeight: ItemWeight =
    Item.values.foldLeft(ItemWeight.Zero): (acc, item) =>
      if item.weight.value > acc.value then item.weight else acc

  /** @return
    *   the arithmetic mean of all [[Item]] weights, rounded to the nearest
    *   integer.
    */
  def averageWeight: ItemWeight =
    val avg = Item.values.map(_.weight.value).sum / Item.values.length
    ItemWeight(avg.round.toInt)
