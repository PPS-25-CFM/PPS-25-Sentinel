package it.unibo.sentinel.boundary.serialization.schemas

import it.unibo.sentinel.boundary.serialization.Schema
import it.unibo.sentinel.boundary.serialization.Codec.Validation
import it.unibo.sentinel.core.scenario.RobotClass

final case class SpawnSchema(
    id: String,
    position: PositionSchema,
    ofClass: RobotClass
) extends Schema:

  override def validated: Either[Validation, Schema] =
    position.validated.map(_ => this)
