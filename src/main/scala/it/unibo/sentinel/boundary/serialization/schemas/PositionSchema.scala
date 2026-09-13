package it.unibo.sentinel.boundary.serialization.schemas

import it.unibo.sentinel.boundary.serialization.Schema
import it.unibo.sentinel.boundary.serialization.Codec.Validation

/** Schema of a [[Position]].
  */
case class PositionSchema(x: Int, y: Int) extends Schema:

  override def validated: Either[Validation, PositionSchema] = Right(this)
