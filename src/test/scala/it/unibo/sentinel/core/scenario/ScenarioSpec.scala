package it.unibo.sentinel.core.scenario

import it.unibo.sentinel.UnitTest
import it.unibo.sentinel.core.warehouse.{Warehouse, Position, Area, Tile}
import it.unibo.sentinel.core.robot.RobotId
import it.unibo.sentinel.core.mission.{Mission, MissionId, Task}
import org.mockito.Mockito

class ScenarioSpec extends UnitTest:
  import Validation.*
  "A Scenario" when:
    val width = 5
    val height = 5
    val topCorner = Position(1, 1)
    val bottomCorner = Position(width - 2, height - 2)
    val warehouse = Warehouse
      .empty(width, height)
      .withArea(Area(topCorner, bottomCorner))(Tile.Floor())
    val s0 = Scenario.in(warehouse)

    "created" should:

      "refer to the given Warehouse" in:
        s0.warehouse shouldBe warehouse

      "should not contain any robot" in:
        s0.spawns shouldBe empty

      "should not contain any mission" in:
        s0.missions shouldBe empty

      "have a default routing policy" in:
        s0.routing shouldBe Policies.Routing.Distance

    "place a robot" should:

      "return a new scenario with the robot placed" in:
        val result =
          s0.place(Spawn(id = RobotId("R1"), at = Position(1, 1))).right.value
        result.spawns should contain only Spawn(
          id = RobotId("R1"),
          at = Position(1, 1)
        )

      "signal that the position is occupied" in:
        val position = Position(1, 1)
        val result =
          for
            s1 <- s0.place(Spawn(id = RobotId("R1"), at = position))
            s2 <- s1.place(Spawn(id = RobotId("R2"), at = position))
          yield s2
        result.left.value shouldBe PositionOccupied(Position(1, 1))

      "signal that the position is not a floor tile" in:
        val position = Position(0, 0)
        val result =
          s0.place(Spawn(id = RobotId("R1"), at = position))
        result.left.value shouldBe NotFloorTile(position)

      "signal that the id is already used" in:
        val id = RobotId("R1")
        val result =
          for
            s1 <- s0.place(Spawn(id = id, at = Position(1, 1)))
            s2 <- s1.place(Spawn(id = id, at = Position(1, 2)))
          yield s2
        result.left.value shouldBe RobotAlreadyExists(id)

    "load a mission" should:

      "return a new scenario with the mission added" in:
        val mission = Mission(
          id = MissionId("M1"),
          task = Task.Move(Position(1, 1)),
          duration = 10
        )
        val result = s0.load(mission).right.value
        result.missions should contain only mission

      "signal that the mission id already exists" in:
        val mission = Mission(
          id = MissionId("M1"),
          task = Task.Move(Position(1, 1)),
          duration = 10
        )
        val result =
          for
            s1 <- s0.load(mission)
            s2 <- s1.load(mission)
          yield s2
        result.left.value shouldBe MissionAlreadyExists(mission.id)

    "change the routing policy" should:

      "return a new scenario with the routing policy changed" in:
        val newRouting = Mockito.mock[Policies.Routing]()
        val result = s0.withRouting(newRouting)
        result.routing shouldBe newRouting
