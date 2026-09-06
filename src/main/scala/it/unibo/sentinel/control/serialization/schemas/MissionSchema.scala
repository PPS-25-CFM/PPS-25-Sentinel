package it.unibo.sentinel.control.serialization.schemas

import it.unibo.sentinel.control.serialization.Schema
import it.unibo.sentinel.control.serialization.Codec.Validation
import it.unibo.sentinel.core.mission.Mission
import it.unibo.sentinel.core.mission.MissionId

enum ActionSchema extends Schema:

  case Move(to: PositionSchema)
  case PickUp(target: ItemSchema, at: PositionSchema)
  case Drop(target: ItemSchema, at: PositionSchema)

  override def validated: Either[Validation, Schema] = this match
    case Move(to)           => to.validated.map(_ => this)
    case PickUp(target, at) =>
      for
        _ <- target.validated
        _ <- at.validated
      yield this
    case Drop(target, at) =>
      for
        _ <- target.validated
        _ <- at.validated
      yield this

enum TaskSchema extends Schema:

  case Then(head: TaskSchema, tail: TaskSchema)
  case Single(action: ActionSchema)
  case Done

  override def validated: Either[Validation, Schema] = this match
    case Then(head, tail) =>
      for
        _ <- head.validated
        _ <- tail.validated
      yield this
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
