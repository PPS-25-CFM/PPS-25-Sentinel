package it.unibo.sentinel.control.serialization.schemas

import it.unibo.sentinel.control.serialization.Schema
import it.unibo.sentinel.control.serialization.Codec.Validation
import it.unibo.sentinel.core.scenario.RobotClass

final case class SpawnSchema(
    id: String,
    position: PositionSchema,
    ofClass: RobotClass
) extends Schema:

  override def validated: Either[Validation, Schema] =
    position.validated.map(_ => this)
