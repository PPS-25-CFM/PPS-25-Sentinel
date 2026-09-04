package it.unibo.sentinel.core.item

enum Item(val weight: ItemWeight):
  case Computer   extends Item(ItemWeight(1))
  case Table      extends Item(ItemWeight(10))
  case Fridge     extends Item(ItemWeight(50))
  case Dishwasher extends Item(ItemWeight(50))

object Item:
  def highestWeight: ItemWeight =
    Item.values.foldLeft(ItemWeight.Zero): (acc, item) =>
      if item.weight.value > acc.value then item.weight else acc

  def averageWeight: ItemWeight =
    val avg = Item.values.map(_.weight.value.toDouble).sum / Item.values.length
    ItemWeight(avg.round.toInt)