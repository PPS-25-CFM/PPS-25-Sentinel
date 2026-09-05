package it.unibo.sentinel.control.serialization

import it.unibo.sentinel.UnitTest
import it.unibo.sentinel.core.warehouse.{Warehouse, Position, Tile, WarehouseId}
import it.unibo.sentinel.control.serialization.schemas.*
import it.unibo.sentinel.control.serialization.converters.*
import it.unibo.sentinel.core.simulation.Tick
import it.unibo.sentinel.core.mission.{Mission, MissionId}

class ConvertersSpec extends UnitTest:

  "A PositionConverter" when:
    behave like basicConverter(
      model = Position(1, 1),
      schema = PositionSchema(1, 1),
      converter = PositionConverter
    )

  "A WarehouseConverter" when:
    val model: Warehouse = Warehouse
      .empty(WarehouseId("W"), 3, 3)
      .withTile(Position(1, 1))(Tile.Floor(Tick.unit))
    val schema: WarehouseSchema =
      WarehouseSchema(
        "W",
        3,
        3,
        Seq(PositionSchema(1, 1) -> TileSchema.Floor(1))
      )
    behave like basicConverter(
      model = model,
      schema = schema,
      WarehouseConverter
    )

  "A MissionConverter" when:
    behave like basicConverter(
      model = Mission.relocate(MissionId("M1"), Position(5, 5), Tick(10)),
      schema = MissionSchema(
        "M1",
        TaskSchema.Single(ActionSchema.Move(PositionSchema(5, 5))),
        10
      ),
      converter = MissionConverter
    )

  private def basicConverter[M, S](
      model: M,
      schema: S,
      converter: Converter[M, S]
  ): Unit =
    "convert from domain model to schema correctly" in:
      converter.toSchema(model) shouldBe schema

    "convert from schema to domain model correctly" in:
      converter.toDomain(schema) shouldBe Right(model)
