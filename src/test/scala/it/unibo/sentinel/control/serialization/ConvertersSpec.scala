package it.unibo.sentinel.control.serialization

import it.unibo.sentinel.UnitTest
import it.unibo.sentinel.core.item.Item
import it.unibo.sentinel.core.mission.{Mission, MissionId, Priority, Task}
import it.unibo.sentinel.core.robot.RobotId
import it.unibo.sentinel.core.robot.value
import it.unibo.sentinel.core.scenario.{RobotClass, Spawn}
import it.unibo.sentinel.core.simulation.Tick
import it.unibo.sentinel.core.warehouse.{Position, Tile, Warehouse, WarehouseId}
import it.unibo.sentinel.control.serialization.Codec.Validation
import it.unibo.sentinel.control.serialization.converters.*
import it.unibo.sentinel.control.serialization.converters.WarehouseConverter.given
import it.unibo.sentinel.control.serialization.schemas.*

class ConvertersSpec extends UnitTest:

  "A PositionConverter" when:
    behave like basicConverter(
      model = Position(1, 1),
      schema = PositionSchema(1, 1),
      converter = PositionConverter
    )

  "An ItemConverter" when:
    behave like basicConverter(
      model = Item.Computer,
      schema = ItemSchema.Computer(1.0),
      converter = ItemConverter
    )

    "convert all item kinds preserving identity" in:
      Seq(Item.Computer, Item.Table, Item.Fridge, Item.Dishwasher).foreach:
        item =>
          ItemConverter
            .toDomain(ItemConverter.toSchema(item))
            .shouldBe(Right(item))

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
      converter = WarehouseConverter
    )

    "encode and decode a Shelf tile" in:
      val tileConverter = summon[Converter[Tile, TileSchema]]
      val tileModel: Tile = Tile.Shelf(Item.Fridge)
      val tileSchema: TileSchema = TileSchema.Shelf(ItemSchema.Fridge(50.0))
      tileConverter.toSchema(tileModel).shouldBe(tileSchema)
      tileConverter.toDomain(tileSchema).shouldBe(Right(tileModel))

    "encode and decode a LoadingBay tile" in:
      val tileConverter = summon[Converter[Tile, TileSchema]]
      val tileModel: Tile = Tile.LoadingBay(Tick(3))
      val tileSchema: TileSchema = TileSchema.LoadingBay(3)
      tileConverter.toSchema(tileModel).shouldBe(tileSchema)
      tileConverter.toDomain(tileSchema).shouldBe(Right(tileModel))

    "encode and decode a warehouse containing Shelf and LoadingBay" in:
      val richModel = Warehouse
        .empty(WarehouseId("W"), 3, 3)
        .withTile(Position(0, 0))(Tile.Shelf(Item.Table))
        .withTile(Position(2, 2))(Tile.LoadingBay(Tick(2)))
      val richSchema = WarehouseSchema(
        "W",
        3,
        3,
        Seq(
          PositionSchema(0, 0) -> TileSchema.Shelf(ItemSchema.Table(10.0)),
          PositionSchema(2, 2) -> TileSchema.LoadingBay(2)
        )
      )
      WarehouseConverter.toSchema(richModel).shouldBe(richSchema)
      WarehouseConverter.toDomain(richSchema).shouldBe(Right(richModel))

  "A MissionConverter" when:
    behave like basicConverter(
      model = Mission
        .relocate(MissionId("M1"), Position(5, 5), Tick(10), Priority(2)),
      schema = MissionSchema(
        "M1",
        TaskSchema.Single(ActionSchema.Move(PositionSchema(5, 5))),
        10,
        2
      ),
      converter = MissionConverter
    )

    "encode and decode a PickUp mission" in:
      val pickModel = Mission(
        MissionId("M2"),
        Task.pick(Item.Computer, Position(1, 1)),
        Tick(5),
        Priority(1)
      )
      val pickSchema = MissionSchema(
        "M2",
        TaskSchema.Single(
          ActionSchema.PickUp(ItemSchema.Computer(1.0), PositionSchema(1, 1))
        ),
        5,
        1
      )
      MissionConverter.toSchema(pickModel).shouldBe(pickSchema)
      MissionConverter.toDomain(pickSchema).shouldBe(Right(pickModel))

    "encode and decode a Drop mission" in:
      val dropModel =
        Mission(MissionId("M3"), Task.drop(Item.Table, Position(2, 2)), Tick(5))
      val dropSchema = MissionSchema(
        "M3",
        TaskSchema.Single(
          ActionSchema.Drop(ItemSchema.Table(10.0), PositionSchema(2, 2))
        ),
        5
      )
      MissionConverter.toSchema(dropModel).shouldBe(dropSchema)
      MissionConverter.toDomain(dropSchema).shouldBe(Right(dropModel))

    "encode and decode a Then (pickAndDrop) mission" in:
      val thenModel = Mission.deliver(
        MissionId("M4"),
        Item.Computer,
        Position(1, 1),
        Position(2, 2),
        Tick(10),
        Priority(5)
      )
      val thenSchema = MissionSchema(
        "M4",
        TaskSchema.Then(
          TaskSchema.Single(
            ActionSchema.PickUp(ItemSchema.Computer(1.0), PositionSchema(1, 1))
          ),
          TaskSchema.Single(
            ActionSchema.Drop(ItemSchema.Computer(1.0), PositionSchema(2, 2))
          )
        ),
        10,
        5
      )
      MissionConverter.toSchema(thenModel).shouldBe(thenSchema)
      MissionConverter.toDomain(thenSchema).shouldBe(Right(thenModel))

    "reject out-of-range priority as InvalidPriority" in:
      val badSchema = MissionSchema(
        "M1",
        TaskSchema.Single(ActionSchema.Move(PositionSchema(5, 5))),
        10,
        0
      )
      MissionConverter
        .toDomain(badSchema)
        .shouldBe(
          Left(
            Validation.MissionValidation(
              Mission.Validation.InvalidPriority(MissionId("M1"), 0)
            )
          )
        )

    "reject TaskSchema.Done as AlreadyCompleted" in:
      MissionConverter
        .toDomain(MissionSchema("M9", TaskSchema.Done, 10))
        .shouldBe(
          Left(
            Validation.MissionValidation(
              Mission.Validation.AlreadyCompleted(MissionId("M9"))
            )
          )
        )

  "A Spawn conversion" when:
    "preserve RobotClass and position in SpawnSchema" in:
      val spawn = Spawn(RobotId("R1"), Position(1, 1), RobotClass.HeavyCarrier)
      val schema =
        SpawnSchema("R1", PositionSchema(1, 1), RobotClass.HeavyCarrier)
      schema.id.shouldBe(spawn.id.value)
      schema.position.shouldBe(PositionConverter.toSchema(spawn.at))
      schema.ofClass.shouldBe(spawn.ofClass)

  private def basicConverter[M, S](
      model: M,
      schema: S,
      converter: Converter[M, S]
  ): Unit =
    "convert from domain model to schema correctly" in:
      converter.toSchema(model).shouldBe(schema)

    "convert from schema to domain model correctly" in:
      converter.toDomain(schema).shouldBe(Right(model))
