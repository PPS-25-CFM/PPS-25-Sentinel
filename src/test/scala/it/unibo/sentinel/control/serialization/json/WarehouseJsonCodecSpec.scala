package it.unibo.sentinel.control.serialization.json

import it.unibo.sentinel.UnitTest
import it.unibo.sentinel.core.warehouse.{
  Warehouse,
  Position,
  Tile,
  Area,
  WarehouseId
}
import it.unibo.sentinel.core.simulation.Tick
import it.unibo.sentinel.core.item.Item
import it.unibo.sentinel.control.serialization.Codec.Validation
import it.unibo.sentinel.control.serialization.Codec
import it.unibo.sentinel.control.serialization.JsonSerialization.given

class WarehouseJsonCodecSpec extends UnitTest:

  val id: WarehouseId = WarehouseId("w01")
  val correctWarehouse: Warehouse = Warehouse
    .empty(id, 4, 4)
    .withArea(Area(Position(1, 1), Position(2, 2))):
      Tile.Floor(Tick.unit)

  val codec = summon[Codec[Warehouse]]
  val validJsonInput: String = codec.encode(correctWarehouse)

  "The WarehouseJsonCodec" should:

    "correctly decode a valid JSON into a Warehouse domain object" in:
      val result = codec.decode(validJsonInput)
      result shouldBe Right(correctWarehouse)

    "correctly encode a Warehouse domain object into JSON" in:
      val jsonOutput = codec.encode(correctWarehouse)
      val parsedResult = codec.decode(jsonOutput)
      parsedResult shouldBe Right(correctWarehouse)

    "roundtrip encode and decode seamlessly" in:
      val jsonOutput = codec.encode(correctWarehouse)
      val decoded = codec.decode(jsonOutput)
      decoded shouldBe Right(correctWarehouse)

    "return SyntaxError when given invalid JSON syntax" in:
      val malformedJson = """{ "width": 4, "height": 4, "tiles": """
      val result = codec.decode(malformedJson)
      result should matchPattern { case Left(Validation.Syntax(_)) => }

    "return WarehouseValidation(InvalidSize) when dimensions are non-positive" in:
      val invalidDimJson =
        s"""{
          |  "id": "$id",
          |  "width": 0,
          |  "height": -2,
          |  "tiles": []
          |}""".stripMargin
      val result = codec.decode(invalidDimJson)
      result shouldBe Left(
        Validation.WarehouseValidation(
          Warehouse.Validation.InvalidSize(0, -2)
        )
      )

    "return WarehouseValidation(TilesOutOfBounds) when a tile position is out of grid bounds" in:
      val outOfBoundsJson =
        s"""{
          |  "id": "$id",
          |  "width": 1,
          |  "height": 1,
          |  "tiles": [
          |    [{"x": 5, "y": 5}, {"$$type": "Floor", "cost": 1}]
          |  ]
          |}""".stripMargin
      val result = codec.decode(outOfBoundsJson)
      result shouldBe Left(
        Validation.WarehouseValidation(
          Warehouse.Validation.TilesOutOfBounds(Seq(Position(5, 5)), 1, 1)
        )
      )

    "return TileValidation(NegativeCost) when a tile has a negative cost" in:
      val cost: Int = -5
      val negativeCostJson =
        s"""{
          |  "id": "$id",
          |  "width": 4,
          |  "height": 4,
          |  "tiles": [
          |    [{"x": 1, "y": 1}, {"$$type": "Floor", "cost": $cost}]
          |  ]
          |}""".stripMargin
      val result = codec.decode(negativeCostJson)
      result shouldBe Left(
        Validation.TileValidation:
          Tile.Validation.NegativeCost(cost)
      )

    "encoding and decoding correctly a warehouse containing Shelf and LoadingBay" in:
      val withItems = Warehouse
        .empty(id, 4, 4)
        .withTile(Position(0, 0))(Tile.Shelf(Item.Fridge))
        .withTile(Position(1, 1))(Tile.LoadingBay(Tick(2)))
      codec.decode(codec.encode(withItems)).shouldBe(Right(withItems))

    "return TileValidation(NegativeCost) for LoadingBay with negative cost" in:
      val json =
        s"""{
          |  "id": "$id",
          |  "width": 4,
          |  "height": 4,
          |  "tiles": [
          |    [{"x": 0, "y": 0}, {"$$type": "LoadingBay", "cost": -1}]
          |  ]
          |}""".stripMargin
      codec
        .decode(json)
        .shouldBe(
          Left(Validation.TileValidation(Tile.Validation.NegativeCost(-1)))
        )

    "return ItemValidation(NegativeWeight) for Shelf with invalid item weight" in:
      val json =
        s"""{
          |  "id": "$id",
          |  "width": 4,
          |  "height": 4,
          |  "tiles": [
          |    [{"x": 0, "y": 0}, {"$$type": "Shelf", "item": {"$$type": "Computer", "weight": -1.0}}]
          |  ]
          |}""".stripMargin
      codec
        .decode(json)
        .shouldBe(
          Left(Validation.ItemValidation(Item.Validation.NegativeWeight(-1.0)))
        )
