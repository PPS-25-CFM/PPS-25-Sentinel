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
import it.unibo.sentinel.core.mission.{Task, Priority}
import it.unibo.sentinel.core.simulation.Tick
import it.unibo.sentinel.core.mission.MissionId
import it.unibo.sentinel.core.item.Item

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
  val initial = ScenarioEditor.State(emptyScenario, None)

  "A ScenarioEditor" when:

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

      "signal a failure when placing a robot in an invalid position and not change the scenario" in:
        val notFloor = Position(0, 0)
        val fail =
          ScenarioEditor(initial, Command.PlaceRobot(notFloor, ofClass))
        inside(fail):
          case State(scenario, fail) =>
            scenario shouldBe initial.scenario
            fail should not be empty

    "receives a LoadMission command" should:
      val task = Task.move(p1)
      val deadline = Tick(10)
      val priority = Priority.normal
      val edited =
        ScenarioEditor(initial, Command.LoadMission(task, deadline, priority))

      "load the mission into the scenario" in:
        inside(edited):
          case State(scenario, None) =>
            inside(scenario.missions):
              case Seq(mission) =>
                mission.task shouldBe task
                mission.deadline shouldBe deadline
                mission.priority shouldBe priority

      "compute the mission id automaticcaly" in:
        inside(edited):
          case State(scenario, None) =>
            inside(scenario.missions):
              case Seq(mission) =>
                mission.id shouldBe MissionId("M1")

      "signal a failure when loading a mission with an invalid task and not change the scenario" in:
        val item = Item.Computer
        val invalidTask = Task.pick(item, Position(0, 0))
        val fail =
          ScenarioEditor(
            initial,
            Command.LoadMission(invalidTask, deadline, priority)
          )
        inside(fail):
          case State(scenario, fail) =>
            scenario shouldBe initial.scenario
            fail should not be empty
