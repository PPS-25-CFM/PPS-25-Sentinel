package it.unibo.sentinel.control.serialization.converters

import it.unibo.sentinel.core.warehouse.Position
import it.unibo.sentinel.control.serialization.Converter
import it.unibo.sentinel.control.serialization.schemas.PositionSchema
import it.unibo.sentinel.control.serialization.Codec.Validation

object PositionConverter extends Converter[Position, PositionSchema]:

  override def toSchema(model: Position): PositionSchema =
    PositionSchema(model.x, model.y)

  override def toDomain(schema: PositionSchema): Either[Validation, Position] =
    Right(Position(schema.x, schema.y))
