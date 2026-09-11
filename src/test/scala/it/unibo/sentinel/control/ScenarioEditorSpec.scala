package it.unibo.sentinel.control

import it.unibo.sentinel.UnitTest
import it.unibo.sentinel.core.warehouse.{
  Warehouse,
  WarehouseId,
  Area,
  Tile,
  Position
}
import it.unibo.sentinel.core.scenario.{Scenario, Spawn, RobotClass}
import it.unibo.sentinel.core.robot.RobotId

trait ScenarioEditorFixture:
  val warehouseId = WarehouseId("test")
  val width = 10
  val height = 10
  val p1 = Position(1, 1)
  val p2 = Position(width - 1, height - 1)
  val room = Area(p1, p2)
  val warehouse = Warehouse
    .empty(warehouseId, width, height)
    .withArea(room)(Tile.Floor())
  val emptyScenario = Scenario.in(warehouse)

class ScenarioEditorSpec extends UnitTest with ScenarioEditorFixture:
  import ScenarioEditor.*
  "A ScenarioEditor" when:
    val initial = ScenarioEditor.State(emptyScenario, None)

    "receives a PlaceRobot command" should:
      val at = p1
      val ofClass = RobotClass.Drone
      val edited = ScenarioEditor(initial, Command.PlaceRobot(at, ofClass))

      "place the robot in the scenario" in:
        inside(edited):
          case State(scenario, None) =>
            inside(scenario.spawns):
              case Seq(Spawn(_, pos, cls)) =>
                pos shouldBe at
                cls shouldBe ofClass

      "compute the robot id automaticcaly" in:
        inside(edited):
          case State(scenario, None) =>
            inside(scenario.spawns):
              case Seq(Spawn(rid, _, _)) =>
                rid shouldBe RobotId("R1")