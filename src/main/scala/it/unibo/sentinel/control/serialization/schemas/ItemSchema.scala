package it.unibo.sentinel.control.serialization.schemas

import it.unibo.sentinel.core.item.Item
import it.unibo.sentinel.control.serialization.Schema
import it.unibo.sentinel.control.serialization.Codec.Validation

enum ItemSchema(weight: Double) extends Schema:
  case Computer(weight: Double) extends ItemSchema(weight)
  case Table(weight: Double) extends ItemSchema(weight)
  case Fridge(weight: Double) extends ItemSchema(weight)
  case Dishwasher(weight: Double) extends ItemSchema(weight)

  def validated: Either[Validation, Schema] =
    if weight <= 0 then
      Left(Validation.ItemValidation(Item.Validation.NegativeWeight(weight)))
    else Right(this)
