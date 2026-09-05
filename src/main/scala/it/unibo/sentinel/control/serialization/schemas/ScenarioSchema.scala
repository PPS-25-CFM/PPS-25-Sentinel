package it.unibo.sentinel.control.serialization.schemas

import it.unibo.sentinel.control.serialization.Schema
import it.unibo.sentinel.control.serialization.Codec.Validation
import it.unibo.sentinel.core.robot.RobotId
import it.unibo.sentinel.core.mission.MissionId
import it.unibo.sentinel.control.serialization.converters.PositionConverter
import it.unibo.sentinel.core.scenario.Validation as DomainValidation
import it.unibo.sentinel.core.scenario.Policies.Routing
import it.unibo.sentinel.core.scenario.Policies.Assignment
import it.unibo.sentinel.core.scenario.Policies.CollisionSelection
import it.unibo.sentinel.core.scenario.Policies.CollisionAvoidance
import it.unibo.sentinel.control.serialization.validateAll

extension [A](seq: Seq[A])
  private def getFirstDuplicate: Option[A] =
    seq.filter(e => seq.count(_ == e) > 1).headOption

final case class ScenarioSchema(
    id: String,
    warehouseId: String,
    spawns: Seq[SpawnSchema],
    missions: Seq[MissionSchema],
    routing: Routing,
    assignment: Assignment,
    collisionSelection: CollisionSelection,
    collisionAvoidance: CollisionAvoidance
) extends Schema:

  override def validated: Either[Validation, ScenarioSchema] =
    for
      _ <- spawns.validateAll
      _ <- missions.validateAll
      _ <- checkUniqueBy(spawns)(
        _.id,
        id =>
          Validation.ScenarioValidation:
            DomainValidation.RobotAlreadyExists(RobotId(id))
      )
      _ <- checkUniqueBy(spawns)(
        _.position,
        pos =>
          PositionConverter.toDomain(pos) match
            case Left(validation) => validation
            case Right(value)     =>
              Validation.ScenarioValidation:
                DomainValidation.PositionOccupied(value)
      )
      _ <- checkUniqueBy(missions)(
        _.id,
        id =>
          Validation.ScenarioValidation:
            DomainValidation.MissionAlreadyExists(MissionId(id))
      )
    yield this

  private def checkUniqueBy[A, Key](items: Seq[A])(
      extractKey: A => Key,
      makeError: Key => Validation
  ): Either[Validation, Unit] =
    items.map(extractKey).getFirstDuplicate match
      case Some(duplicateKey) =>
        Left(makeError(duplicateKey))
      case None =>
        Right(())
