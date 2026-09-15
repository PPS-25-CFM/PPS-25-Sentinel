package it.unibo.sentinel.boundary.serialization.json

import it.unibo.sentinel.UnitTest
import it.unibo.sentinel.core.warehouse.{
  Warehouse,
  Position,
  Tile,
  Area,
  WarehouseId
}
import it.unibo.sentinel.core.simulation.Tick
import it.unibo.sentinel.core.scenario.{Scenario, Spawn}
import it.unibo.sentinel.core.robot.RobotId
import it.unibo.sentinel.core.mission.MissionId
import it.unibo.sentinel.boundary.serialization.Codec
import it.unibo.sentinel.boundary.persistence.Repository
import it.unibo.sentinel.boundary.serialization.Codec.Validation
import it.unibo.sentinel.boundary.serialization.JsonSerialization.given
import it.unibo.sentinel.core.mission.Mission
import it.unibo.sentinel.core.scenario.ScenarioId
import it.unibo.sentinel.core.item.Item
import it.unibo.sentinel.core.scenario.RobotClass

class ScenarioJsonCodecSpec extends UnitTest:

  given Repository[os.Path, Warehouse] =
    new Repository[os.Path, Warehouse]:
      override def save(
          model: Warehouse,
          key: os.Path
      ): Either[Validation, Unit] = Left(Validation.FileNotFound(""))
      override def load(key: os.Path): Either[Validation, Warehouse] = Left(
        Validation.FileNotFound("")
      )

  val pickPos: Position = Position(0, 0)
  val bayPos: Position = Position(4, 4)

  val testWarehouse: Warehouse = Warehouse
    .empty(WarehouseId("W"), 5, 5)
    .withArea(Area(Position(1, 1), Position(3, 3))):
      Tile.Floor(Tick.unit)
    .withTile(pickPos)(Tile.Shelf(Item.Computer))
    .withTile(bayPos)(Tile.LoadingBay())

  val scenario: Scenario = Scenario
    .in(testWarehouse)
    .withId(ScenarioId("S1"))
    .place(Spawn(RobotId("R1"), Position(1, 1), RobotClass.Drone))
    .getOrElse(fail("Could not place spawn"))
    .load(Mission.relocate(MissionId("M1"), Position(2, 2), Tick(10)))
    .getOrElse(fail("Could not load mission"))

  given (String => Either[Validation, Warehouse]) = _ => Right(testWarehouse)
  val codec: Codec[Scenario] = summon[Codec[Scenario]]
  val json: String =
    s"""{
       |  "id": "${scenario.id}",
       |  "warehouseId": "${scenario.warehouse.id}",
       |  "spawns": [
       |    {
       |      "id": "R1",
       |      "position": {
       |        "x": 1,
       |        "y": 1
       |      },
       |      "ofClass": "Drone"
       |    }
       |  ],
       |  "missions": [
       |    {
       |      "id": "M1",
       |      "task": {
       |        "$$type": "Single",
       |        "action": {
       |          "$$type": "Move",
       |          "to": {
       |            "x": 2,
       |            "y": 2
       |          }
       |        }
       |      },
       |      "duration": 10
       |    }
       |  ],
       |  "routing": "Distance",
       |  "assignment": "Nearest",
       |  "collisionSelection": "Random",
       |  "collisionAvoidance": "Wait"
       |}""".stripMargin.replaceAll("\\s+", "")

  "The ScenarioJsonCodec" should:

    "correctly encode a valid Scenario domain object into JSON format" in:
      codec.encode(scenario) shouldBe json

    "correctly decode a valid JSON into a Scenario domain object" in:
      codec.decode(json) shouldBe Right(scenario)

    "return a Validation error when decoding invalid JSON syntax" in:
      val invalidJson = "{ invalid json }"
      codec.decode(invalidJson).isLeft shouldBe true
