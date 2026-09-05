package it.unibo.sentinel.control.serialization.schemas

import it.unibo.sentinel.control.serialization.Schema
import it.unibo.sentinel.control.serialization.Codec.Validation
import it.unibo.sentinel.core.mission.Mission
import it.unibo.sentinel.core.mission.MissionId

enum ActionSchema extends Schema:

  case Move(to: PositionSchema)

  override def validated: Either[Validation, Schema] = this match
    case Move(to) => to.validated.map(_ => this)

enum TaskSchema extends Schema:

  case Single(action: ActionSchema)
  case Done

  override def validated: Either[Validation, Schema] = this match
    case Single(action) => action.validated.map(_ => this)
    case Done           => Right(this)

final case class MissionSchema(id: String, task: TaskSchema, duration: Int)
    extends Schema:

  override def validated: Either[Validation, Schema] =
    for
      _ <- task.validated
      _ <- Either.cond(
        duration > 0,
        (),
        Validation.MissionValidation:
          Mission.Validation.NegativeDuration(MissionId(id), duration)
      )
    yield this
