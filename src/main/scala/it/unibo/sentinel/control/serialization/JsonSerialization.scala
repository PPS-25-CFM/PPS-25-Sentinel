package it.unibo.sentinel.control.serialization

import it.unibo.sentinel.control.serialization.{Codec, Converter, Schema}
import it.unibo.sentinel.control.serialization.Codec.Validation
import upickle.default.{ReadWriter, read, write, macroRW}
import scala.util.Try
import it.unibo.sentinel.core.warehouse.Warehouse
import it.unibo.sentinel.core.warehouse.value
import it.unibo.sentinel.control.serialization.schemas.*
import it.unibo.sentinel.control.serialization.converters.*
import it.unibo.sentinel.core.scenario.Policies.*
import it.unibo.sentinel.core.scenario.Scenario
import it.unibo.sentinel.core.scenario.value
import it.unibo.sentinel.control.serialization.converters.ScenarioConverter.given

object JsonSerialization:

  private final class JsonCodec[Model, ModelSchema <: Schema: ReadWriter](using
      converter: Converter[Model, ModelSchema]
  ) extends Codec[Model]:

    override def encode(model: Model): String =
      write(converter.toSchema(model))

    override def decode(input: String): Either[Validation, Model] =
      parseJson(input)
        .flatMap(schema => schema.validated.map(_ => schema))
        .flatMap(converter.toDomain)

    protected def parseJson(
        input: String
    ): Either[Validation, ModelSchema] =
      Try(read[ModelSchema](input)).toEither.left.map: e =>
        Validation.Syntax(e.getMessage())

  given ReadWriter[PositionSchema] = macroRW
  given ReadWriter[TileSchema.Floor] = macroRW
  given ReadWriter[TileSchema] = macroRW
  given ReadWriter[WarehouseSchema] = macroRW
  given ReadWriter[SpawnSchema] = macroRW
  given ReadWriter[ActionSchema.Move] = macroRW
  given ReadWriter[ActionSchema] = macroRW
  given ReadWriter[TaskSchema.Single] = macroRW
  given ReadWriter[TaskSchema.Done.type] = macroRW
  given ReadWriter[TaskSchema] = macroRW
  given ReadWriter[MissionSchema] = macroRW
  given ReadWriter[Routing.Distance.type] = macroRW
  given ReadWriter[Routing.Time.type] = macroRW
  given ReadWriter[Routing] = macroRW
  given ReadWriter[Assignment.Nearest.type] = macroRW
  given ReadWriter[Assignment] = macroRW
  given ReadWriter[CollisionSelection.Random.type] = macroRW
  given ReadWriter[CollisionSelection] = macroRW
  given ReadWriter[CollisionAvoidance.Wait.type] = macroRW
  given ReadWriter[CollisionAvoidance] = macroRW
  given ReadWriter[ScenarioSchema] = macroRW

  given Converter[Warehouse, WarehouseSchema] = WarehouseConverter
  given Codec[Warehouse] = new JsonCodec[Warehouse, WarehouseSchema]

  given extension: String = ".json"
  given scenarioId: (Warehouse => String) = (w: Warehouse) => w.id.value
  given warehouseId: (Scenario => String) = (s: Scenario) => s.id.value

  given (using repo: FileRepository[Warehouse]): Codec[Scenario] =
    new JsonCodec[Scenario, ScenarioSchema]
