package it.unibo.sentinel.control.serialization.converters

import it.unibo.sentinel.control.serialization.Converter
import it.unibo.sentinel.control.serialization.schemas.MissionSchema
import it.unibo.sentinel.core.mission.{
  Mission,
  MissionId,
  Action,
  Task,
  Priority
}
import it.unibo.sentinel.control.serialization.schemas.TaskSchema
import it.unibo.sentinel.control.serialization.schemas.ActionSchema
import it.unibo.sentinel.core.simulation.Tick
import it.unibo.sentinel.control.serialization.Codec.Validation

object MissionConverter extends Converter[Mission, MissionSchema]:

  private given actionConverter: Converter[Action, ActionSchema] =
    new Converter[Action, ActionSchema]:

      override def toSchema(model: Action): ActionSchema = model match
        case Action.Move(to) =>
          ActionSchema.Move(PositionConverter.toSchema(to))
        case Action.PickUp(target, at) =>
          ActionSchema.PickUp(
            ItemConverter.toSchema(target),
            PositionConverter.toSchema(at)
          )
        case Action.Drop(target, at) =>
          ActionSchema.Drop(
            ItemConverter.toSchema(target),
            PositionConverter.toSchema(at)
          )

      override def toDomain(schema: ActionSchema): Either[Validation, Action] =
        schema match
          case ActionSchema.Move(to) =>
            for pos <- PositionConverter.toDomain(to)
            yield Action.Move(pos)
          case ActionSchema.PickUp(target, at) =>
            for
              item <- ItemConverter.toDomain(target)
              pos <- PositionConverter.toDomain(at)
            yield Action.PickUp(item, pos)
          case ActionSchema.Drop(target, at) =>
            for
              item <- ItemConverter.toDomain(target)
              pos <- PositionConverter.toDomain(at)
            yield Action.Drop(item, pos)

  private given taskConverter: Converter[Task, TaskSchema] =
    new Converter[Task, TaskSchema]:

      override def toSchema(model: Task): TaskSchema = model match
        case Task.Then(head, tail) =>
          TaskSchema.Then(toSchema(head), toSchema(tail))
        case Task.Single(action) =>
          TaskSchema.Single(actionConverter.toSchema(action))
        case Task.Done => TaskSchema.Done

      override def toDomain(schema: TaskSchema): Either[Validation, Task] =
        schema match
          case TaskSchema.Then(head, tail) =>
            for
              domainHead <- toDomain(head)
              domainTail <- toDomain(tail)
            yield Task.Then(domainHead, domainTail)
          case TaskSchema.Single(action) =>
            for domainAction <- actionConverter.toDomain(action)
            yield Task.Single(domainAction)
          case TaskSchema.Done =>
            Right(Task.Done)

  override def toSchema(model: Mission): MissionSchema =
    MissionSchema(
      model.id.value,
      taskConverter.toSchema(model.task),
      model.deadline.value,
      model.priority.value
    )

  override def toDomain(schema: MissionSchema): Either[Validation, Mission] =
    for
      domainTask <- taskConverter.toDomain(schema.task)
      priority <- Priority
        .from(schema.priority)
        .toRight(
          Validation.MissionValidation(
            Mission.Validation.InvalidPriority(
              MissionId(schema.id),
              schema.priority
            )
          )
        )
      mission <- domainTask match
        case Task.Done =>
          Left(
            Validation.MissionValidation(
              Mission.Validation.AlreadyCompleted(MissionId(schema.id))
            )
          )
        case validTask =>
          Right(
            Mission(
              MissionId(schema.id),
              validTask,
              Tick(schema.duration),
              priority
            )
          )
    yield mission
