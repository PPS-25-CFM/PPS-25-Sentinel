package it.unibo.sentinel.boundary.serialization.converters

import it.unibo.sentinel.boundary.serialization.Converter
import it.unibo.sentinel.boundary.serialization.schemas.ScenarioSchema
import it.unibo.sentinel.core.scenario.Spawn
import it.unibo.sentinel.boundary.serialization.schemas.SpawnSchema
import it.unibo.sentinel.core.robot.RobotId
import it.unibo.sentinel.core.robot.value
import it.unibo.sentinel.core.mission.Mission
import it.unibo.sentinel.core.warehouse.Warehouse
import it.unibo.sentinel.core.warehouse.value
import it.unibo.sentinel.core.scenario.Scenario
import it.unibo.sentinel.boundary.serialization.Codec.Validation
import it.unibo.sentinel.boundary.serialization.schemas.MissionSchema
import it.unibo.sentinel.core.scenario.Validation as ScenarioValidation
import it.unibo.sentinel.core.scenario.value
import it.unibo.sentinel.core.scenario.ScenarioId

object ScenarioConverter:

  private given spawnConverter: Converter[Spawn, SpawnSchema] =
    new Converter[Spawn, SpawnSchema]:

      override def toSchema(model: Spawn): SpawnSchema =
        SpawnSchema(
          model.id.value,
          PositionConverter.toSchema(model.at),
          model.ofClass
        )

      override def toDomain(schema: SpawnSchema): Either[Validation, Spawn] =
        for pos <- PositionConverter.toDomain(schema.position)
        yield Spawn(RobotId(schema.id), pos, schema.ofClass)

  private given missionConverter: Converter[Mission, MissionSchema] =
    MissionConverter

  given scenarioConverter(using
      warehouse: String => Either[Validation, Warehouse]
  ): Converter[Scenario, ScenarioSchema] with

    override def toSchema(model: Scenario): ScenarioSchema =
      ScenarioSchema(
        model.id.value,
        model.warehouse.id.value,
        model.spawns.map(spawnConverter.toSchema),
        model.missions.map(missionConverter.toSchema),
        model.routing,
        model.assignment,
        model.collisionSelection,
        model.collisionAvoidance,
        model.seed
      )

    override def toDomain(
        schema: ScenarioSchema
    ): Either[Validation, Scenario] =
      for
        warehouse <- warehouse(schema.warehouseId)
        initialScenario = Scenario.in(warehouse).withSeed(schema.seed)
        scenarioWithSpawns <- loadValues[Spawn, SpawnSchema](
          initialScenario,
          schema.spawns
        )((s, spawn) => s.place(spawn))
        scenarioWithMissions <- loadValues[Mission, MissionSchema](
          scenarioWithSpawns,
          schema.missions
        )((s, mission) => s.load(mission))
      yield scenarioWithMissions
        .withId(ScenarioId(schema.id))
        .withRouting(schema.routing)
        .withAssignment(schema.assignment)
        .withCollisionSelection(schema.collisionSelection)
        .withCollisionAvoidance(schema.collisionAvoidance)

    private def loadValues[A, B](start: Scenario, values: Seq[B])(
        stepper: (Scenario, A) => Either[ScenarioValidation, Scenario]
    )(using converter: Converter[A, B]): Either[Validation, Scenario] =
      values.foldLeft[Either[Validation, Scenario]](Right(start)) {
        (acc, schema) =>
          for
            scenario <- acc
            domainObj <- converter.toDomain(schema)
            nextScenario <- stepper(scenario, domainObj).left.map(
              Validation.ScenarioValidation(_)
            )
          yield nextScenario
      }
