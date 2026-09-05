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
import it.unibo.sentinel.core.scenario.{Scenario, Spawn}
import it.unibo.sentinel.core.robot.RobotId
import it.unibo.sentinel.core.mission.MissionId
import it.unibo.sentinel.control.serialization.Codec
import it.unibo.sentinel.control.serialization.JsonSerialization.given
import it.unibo.sentinel.core.mission.Mission
import it.unibo.sentinel.core.scenario.ScenarioId
import it.unibo.sentinel.control.serialization.FileRepository

class ScenarioJsonCodecSpec extends UnitTest:

  given warehouseName: String = "test-warehouse.json"

  val testWarehouse: Warehouse = Warehouse
    .empty(WarehouseId("W"), 5, 5)
    .withArea(Area(Position(1, 1), Position(3, 3))):
      Tile.Floor(Tick.unit)

  val scenario: Scenario = Scenario
    .in(testWarehouse)
    .withId(ScenarioId("S1"))
    .place(Spawn(RobotId("R1"), Position(1, 1)))
    .getOrElse(fail("Could not place spawn"))
    .load(Mission.relocate(MissionId("M1"), Position(2, 2), Tick(10)))
    .getOrElse(fail("Could not load mission"))

  given FileRepository[Warehouse] = new FileRepository[Warehouse]
  val codec: Codec[Scenario] = summon[Codec[Scenario]]

  "The ScenarioJsonCodec" should:

    "correctly encode and decode a valid Scenario domain object" in:
      codec.encode(scenario) shouldBe
        s"""{
          |  "id": "${scenario.id}",
          |  "warehouseId": "${scenario.warehouse.id}",
          |  "spawns": [
          |    {
          |      "id": "R1",
          |      "position": {
          |        "x": 1,
          |        "y": 1
          |      }
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
