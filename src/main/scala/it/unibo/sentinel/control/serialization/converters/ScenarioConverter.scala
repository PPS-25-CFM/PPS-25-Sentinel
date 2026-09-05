package it.unibo.sentinel.control.serialization.converters

import it.unibo.sentinel.control.serialization.Converter
import it.unibo.sentinel.control.serialization.schemas.ScenarioSchema
import it.unibo.sentinel.core.scenario.Spawn
import it.unibo.sentinel.control.serialization.schemas.SpawnSchema
import it.unibo.sentinel.core.robot.RobotId
import it.unibo.sentinel.core.robot.value
import it.unibo.sentinel.core.mission.Mission
import it.unibo.sentinel.core.warehouse.Warehouse
import it.unibo.sentinel.core.warehouse.value
import it.unibo.sentinel.core.scenario.Scenario
import it.unibo.sentinel.control.serialization.Repository
import it.unibo.sentinel.control.serialization.Codec.Validation
import it.unibo.sentinel.control.serialization.schemas.MissionSchema
import it.unibo.sentinel.core.scenario.Validation as ScenarioValidation
import it.unibo.sentinel.core.scenario.value
import it.unibo.sentinel.core.scenario.ScenarioId

object ScenarioConverter:

  private given spawnConverter: Converter[Spawn, SpawnSchema] =
    new Converter[Spawn, SpawnSchema]:

      override def toSchema(model: Spawn): SpawnSchema =
        SpawnSchema(model.id.value, PositionConverter.toSchema(model.at))

      override def toDomain(schema: SpawnSchema): Either[Validation, Spawn] =
        for pos <- PositionConverter.toDomain(schema.position)
        yield Spawn(RobotId(schema.id), pos)

  private given missionConverter: Converter[Mission, MissionSchema] =
    MissionConverter

  given (using repo: Repository[String, Warehouse]): Converter[
    Scenario,
    ScenarioSchema
  ] with

    override def toSchema(model: Scenario): ScenarioSchema =
      ScenarioSchema(
        model.id.value,
        model.warehouse.id.value,
        model.spawns.map(spawnConverter.toSchema),
        model.missions.map(missionConverter.toSchema),
        model.routing,
        model.assignment,
        model.collisionSelection,
        model.collisionAvoidance
      )

    override def toDomain(
        schema: ScenarioSchema
    ): Either[Validation, Scenario] =
      repo.load(schema.warehouseId).map { warehouse =>
        var scenario = Scenario.in(warehouse)
        scenario = loadValues[Spawn, SpawnSchema](scenario, schema.spawns) {
          (s, spawn) => s.place(spawn)
        }
        scenario =
          loadValues[Mission, MissionSchema](scenario, schema.missions) {
            (s, mission) => s.load(mission)
          }
        scenario
          .withId(ScenarioId(schema.id))
          .withRouting(schema.routing)
          .withAssignment(schema.assignment)
          .withCollisionSelection(schema.collisionSelection)
          .withCollisionAvoidance(schema.collisionAvoidance)
      }

    private def loadValues[A, B](start: Scenario, values: Seq[B])(
        stepper: (Scenario, A) => Either[ScenarioValidation, Scenario]
    )(using converter: Converter[A, B]): Scenario =
      var scenario: Scenario = start
      for
        either <- values.map(converter.toDomain)
        value <- either
        step <- stepper(scenario, value)
      do scenario = step
      scenario
