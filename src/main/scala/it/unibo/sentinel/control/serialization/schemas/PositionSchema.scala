package it.unibo.sentinel.control.serialization.schemas

import it.unibo.sentinel.control.serialization.Schema
import it.unibo.sentinel.control.serialization.Codec.Validation

/** Schema of a [[Position]].
  */
case class PositionSchema(x: Int, y: Int) extends Schema:

  override def validated: Either[Validation, PositionSchema] = Right(this)
