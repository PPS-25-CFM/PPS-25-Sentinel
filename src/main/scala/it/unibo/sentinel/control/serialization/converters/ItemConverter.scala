package it.unibo.sentinel.control.serialization.converters

import _root_.it.unibo.sentinel.control.serialization.Converter
import it.unibo.sentinel.core.item.Item
import it.unibo.sentinel.control.serialization.schemas.ItemSchema
import it.unibo.sentinel.control.serialization.Codec.Validation

object ItemConverter extends Converter[Item, ItemSchema]:

  override def toSchema(model: Item): ItemSchema =
    model match
      case Item.Computer   => ItemSchema.Computer(model.weight.value)
      case Item.Table      => ItemSchema.Table(model.weight.value)
      case Item.Fridge     => ItemSchema.Fridge(model.weight.value)
      case Item.Dishwasher => ItemSchema.Dishwasher(model.weight.value)

  override def toDomain(schema: ItemSchema): Either[Validation, Item] =
    schema match
      case ItemSchema.Computer(_)   => Right(Item.Computer)
      case ItemSchema.Table(_)      => Right(Item.Table)
      case ItemSchema.Fridge(_)     => Right(Item.Fridge)
      case ItemSchema.Dishwasher(_) => Right(Item.Dishwasher)
