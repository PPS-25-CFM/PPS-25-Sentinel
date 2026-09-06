package it.unibo.sentinel.core.item

enum Item(val weight: ItemWeight):
  case Computer extends Item(ItemWeight(1.0))
  case Table extends Item(ItemWeight(10.0))
  case Fridge extends Item(ItemWeight(50.0))
  case Dishwasher extends Item(ItemWeight(50.0))

object Item:

  enum Validation:
    case NegativeWeight(weight: Double)

  def highestWeight: ItemWeight =
    Item.values.foldLeft(ItemWeight.Zero): (acc, item) =>
      if item.weight.value > acc.value then item.weight else acc

  def averageWeight: ItemWeight =
    val avg = Item.values.map(_.weight.value.toDouble).sum / Item.values.length
    ItemWeight(avg.round.toInt)
