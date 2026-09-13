package it.unibo.sentinel.boundary.serialization

import it.unibo.sentinel.boundary.serialization.{Codec, Converter, Schema}
import it.unibo.sentinel.boundary.serialization.Codec.Validation
import upickle.default.{ReadWriter, read, write}
import scala.util.Try
import it.unibo.sentinel.core.warehouse.Warehouse
import it.unibo.sentinel.boundary.serialization.schemas.*
import it.unibo.sentinel.boundary.serialization.converters.*
import it.unibo.sentinel.core.scenario.Scenario
import it.unibo.sentinel.boundary.serialization.converters.ScenarioConverter.given
import it.unibo.sentinel.core.mission.Mission
import it.unibo.sentinel.core.scenario.RobotClass
import it.unibo.sentinel.core.scenario.Policies.*

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

  given ReadWriter[PositionSchema] = ReadWriter.derived
  given ReadWriter[ItemSchema] = ReadWriter.derived
  given ReadWriter[TileSchema] = ReadWriter.derived
  given ReadWriter[WarehouseSchema] = ReadWriter.derived
  given ReadWriter[SpawnSchema] = ReadWriter.derived
  given ReadWriter[RobotClass] = ReadWriter.derived
  given ReadWriter[ActionSchema] = ReadWriter.derived
  given ReadWriter[TaskSchema] = ReadWriter.derived
  given ReadWriter[MissionSchema] = ReadWriter.derived
  given ReadWriter[Routing] = ReadWriter.derived
  given ReadWriter[Assignment] = ReadWriter.derived
  given ReadWriter[CollisionSelection] = ReadWriter.derived
  given ReadWriter[CollisionAvoidance] = ReadWriter.derived
  given ReadWriter[ScenarioSchema] = ReadWriter.derived

  given Converter[Mission, MissionSchema] = MissionConverter
  given Converter[Warehouse, WarehouseSchema] = WarehouseConverter

  given Codec[Warehouse] = new JsonCodec[Warehouse, WarehouseSchema]

  given (using
      warehouse: String => Either[Validation, Warehouse]
  ): Codec[Scenario] = JsonCodec[Scenario, ScenarioSchema]
