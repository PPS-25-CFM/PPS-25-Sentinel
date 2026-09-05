package it.unibo.sentinel.control.serialization.schemas

import it.unibo.sentinel.control.serialization.Schema
import it.unibo.sentinel.control.serialization.Codec.Validation

final case class SpawnSchema(id: String, position: PositionSchema)
    extends Schema:

  override def validated: Either[Validation, Schema] =
    position.validated.map(_ => this)
