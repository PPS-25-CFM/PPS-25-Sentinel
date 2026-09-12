package it.unibo.sentinel.control

import it.unibo.sentinel.UnitTest
import it.unibo.sentinel.control.ScenarioEditor.{Command, Selection, State}
import it.unibo.sentinel.core.TestData
import it.unibo.sentinel.core.item.Item
import it.unibo.sentinel.core.mission.{MissionId, Priority, Task}
import it.unibo.sentinel.core.robot.RobotId
import it.unibo.sentinel.core.scenario.Policies.{
  Assignment,
  CollisionAvoidance,
  CollisionSelection,
  Routing
}
import it.unibo.sentinel.core.scenario.{RobotClass, Scenario, Validation}
import it.unibo.sentinel.core.simulation.Tick
import it.unibo.sentinel.core.warehouse.{Position, Tile}

trait ScenarioEditorBehaviors:
  self: UnitTest =>

  def aRejectedEdit(
      start: => State,
      command: => Command,
      expected: => Validation
  ): Unit =
    "report the validation that rejected it" in:
      ScenarioEditor(start, command).fail.value shouldBe expected

    "leave the scenario unchanged" in:
      ScenarioEditor(start, command).scenario shouldBe start.scenario

  def anAcceptedEdit(start: => State, command: => Command): Unit =
    "succeed without reporting a failure" in:
      ScenarioEditor(start, command).fail shouldBe empty

trait ScenarioEditorFixture extends TestData:
  self: UnitTest =>
  val floor: Position = Position(1, 1)
  val outside: Position = Position(0, 0)
  val shelf: Position = Position(0, 2)
  val bay: Position = Position(0, 3)
  val stored: Item = Item.Computer
  val deadline: Tick = Tick(10)
  val priority: Priority = Priority.normal

  val scenario: Scenario = Scenario.in(
    warehouse
      .withTile(shelf)(Tile.Shelf(stored))
      .withTile(bay)(Tile.LoadingBay())
  )
  val initial: State = State(scenario)

  val chosen: State = Seq(
    Command.BeginDelivery(shelf),
    Command.Select(bay)
  ).foldLeft(initial)(ScenarioEditor.apply)

class ScenarioEditorSpec
    extends UnitTest
    with ScenarioEditorFixture
    with ScenarioEditorBehaviors:

  "A ScenarioEditor" when:

    "a cell is selected" should:

      "remember the selected cell" in:
        ScenarioEditor(initial, Command.Select(floor)).selection shouldBe
          Selection.Cell(floor)

      "replace a previous selection" in:
        val selected = Seq(
          Command.Select(floor),
          Command.Select(bay)
        ).foldLeft(initial)(ScenarioEditor.apply)
        selected.selection shouldBe Selection.Cell(bay)

      behave like anAcceptedEdit(initial, Command.Select(floor))

    "a robot is placed" should:

      "spawn it at the requested position with the requested class" in:
        val placed =
          ScenarioEditor(initial, Command.PlaceRobot(floor, RobotClass.Drone))
        inside(placed.scenario.spawns):
          case Seq(spawn) =>
            spawn.at shouldBe floor
            spawn.ofClass shouldBe RobotClass.Drone

      "compute the robot id automatically" in:
        val placed =
          ScenarioEditor(initial, Command.PlaceRobot(floor, RobotClass.Drone))
        inside(placed.scenario.spawns):
          case Seq(spawn) => spawn.id shouldBe RobotId("R1")

      "number the following robots progressively" in:
        val placed = Seq(
          Command.PlaceRobot(floor, RobotClass.Drone),
          Command.PlaceRobot(Position(2, 2), RobotClass.Carrier)
        ).foldLeft(initial)(ScenarioEditor.apply)
        placed.scenario.spawns.map(_.id) shouldBe
          Seq(RobotId("R1"), RobotId("R2"))

      behave like anAcceptedEdit(
        initial,
        Command.PlaceRobot(floor, RobotClass.Drone)
      )

      "be rejected on a position that is not a floor" should:
        behave like aRejectedEdit(
          initial,
          Command.PlaceRobot(outside, RobotClass.Drone),
          Validation.NotFloorTile(outside)
        )

    "a robot is removed" should:

      "drop its spawn from the scenario" in:
        val removed = Seq(
          Command.PlaceRobot(floor, RobotClass.Drone),
          Command.RemoveRobot(RobotId("R1"))
        ).foldLeft(initial)(ScenarioEditor.apply)
        removed.scenario.spawns shouldBe empty

      "release the id for the next robot" in:
        val replaced = Seq(
          Command.PlaceRobot(floor, RobotClass.Drone),
          Command.RemoveRobot(RobotId("R1")),
          Command.PlaceRobot(floor, RobotClass.Carrier)
        ).foldLeft(initial)(ScenarioEditor.apply)
        replaced.scenario.spawns.map(_.id) shouldBe Seq(RobotId("R1"))

    "a mission is loaded" should:

      "add it to the scenario with the requested deadline and priority" in:
        val task = Task.move(floor)
        val loaded =
          ScenarioEditor(initial, Command.LoadMission(task, deadline, priority))
        inside(loaded.scenario.missions):
          case Seq(mission) =>
            mission.task shouldBe task
            mission.deadline shouldBe deadline
            mission.priority shouldBe priority

      "compute the mission id automatically" in:
        val loaded = ScenarioEditor(
          initial,
          Command.LoadMission(Task.move(floor), deadline, priority)
        )
        inside(loaded.scenario.missions):
          case Seq(mission) => mission.id shouldBe MissionId("M1")

      "number the following missions progressively" in:
        val loaded = Seq(
          Command.LoadMission(Task.move(floor), deadline, priority),
          Command.LoadMission(Task.move(bay), deadline, priority)
        ).foldLeft(initial)(ScenarioEditor.apply)
        loaded.scenario.missions.map(_.id) shouldBe
          Seq(MissionId("M1"), MissionId("M2"))

      behave like anAcceptedEdit(
        initial,
        Command.LoadMission(Task.move(floor), deadline, priority)
      )

      "be rejected with a task the warehouse cannot satisfy" should:
        behave like aRejectedEdit(
          initial,
          Command.LoadMission(Task.pick(stored, outside), deadline, priority),
          Validation.NotShelfTile(outside)
        )

    "a mission is unloaded" should:

      "drop it from the scenario" in:
        val unloaded = Seq(
          Command.LoadMission(Task.move(floor), deadline, priority),
          Command.UnloadMission(MissionId("M1"))
        ).foldLeft(initial)(ScenarioEditor.apply)
        unloaded.scenario.missions shouldBe empty

    "a relocation is loaded" should:

      "load a movement mission towards the destination" in:
        val loaded = ScenarioEditor(
          initial,
          Command.LoadRelocation(floor, deadline, priority)
        )
        inside(loaded.scenario.missions):
          case Seq(mission) => mission.task shouldBe Task.move(floor)

      behave like anAcceptedEdit(
        initial,
        Command.LoadRelocation(floor, deadline, priority)
      )

      "be rejected if the cell cannot be traversed" should:
        behave like aRejectedEdit(
          initial,
          Command.LoadRelocation(outside, deadline, priority),
          Validation.NotFloorTile(outside)
        )

    "a delivery is begun" should:

      "remember the shelf and await a destination" in:
        val pending = ScenarioEditor(initial, Command.BeginDelivery(shelf))
        pending.selection shouldBe Selection.Delivery(shelf, None)

      behave like anAcceptedEdit(initial, Command.BeginDelivery(shelf))

      "be rejected if the cell is not a shelf" should:
        behave like aRejectedEdit(
          initial,
          Command.BeginDelivery(floor),
          Validation.NotShelfTile(floor)
        )

        "open no delivery" in:
          val refused = ScenarioEditor(initial, Command.BeginDelivery(floor))
          refused.selection.origin shouldBe empty

    "a destination is selected for a pending delivery" should:

      "record it alongside the origin" in:
        chosen.selection shouldBe Selection.Delivery(shelf, Some(bay))

      "reject a cell that is not a loading bay" in:
        val pending = ScenarioEditor(initial, Command.BeginDelivery(shelf))
        val refused = ScenarioEditor(pending, Command.Select(floor))
        refused.fail.value shouldBe Validation.NotLoadingBay(floor)

      "keep the origin after a rejection" in:
        val pending = ScenarioEditor(initial, Command.BeginDelivery(shelf))
        val refused = ScenarioEditor(pending, Command.Select(floor))
        refused.selection shouldBe Selection.Delivery(shelf, None)
        refused.scenario shouldBe initial.scenario

    "a delivery is loaded" should:

      "load one pick-then-drop task using the item stored on the shelf" in:
        val loaded =
          ScenarioEditor(chosen, Command.LoadDelivery(deadline, priority))
        inside(loaded.scenario.missions):
          case Seq(mission) =>
            mission.task shouldBe Task.pickAndDrop(stored, shelf, bay)

      "close the delivery, leaving the destination selected" in:
        val loaded =
          ScenarioEditor(chosen, Command.LoadDelivery(deadline, priority))
        loaded.selection shouldBe Selection.Cell(bay)

      behave like anAcceptedEdit(
        chosen,
        Command.LoadDelivery(deadline, priority)
      )

    "a delivery is cancelled" should:

      "forget the origin but keep the cell selected" in:
        val pending = ScenarioEditor(initial, Command.BeginDelivery(shelf))
        val cancelled = ScenarioEditor(pending, Command.CancelDelivery)
        cancelled.selection shouldBe Selection.Cell(shelf)

      "clear the failure left by an invalid destination" in:
        val refused = ScenarioEditor(
          ScenarioEditor(initial, Command.BeginDelivery(shelf)),
          Command.Select(floor)
        )
        ScenarioEditor(refused, Command.CancelDelivery).fail shouldBe empty

    "a policy is chosen" should:

      "set the routing policy" in:
        ScenarioEditor(
          initial,
          Command.ChooseRouting(Routing.Time)
        ).scenario.routing shouldBe Routing.Time

      "set the assignment policy" in:
        ScenarioEditor(
          initial,
          Command.ChooseAssigmnment(Assignment.Cycle)
        ).scenario.assignment shouldBe Assignment.Cycle

      "set the collision selection policy" in:
        ScenarioEditor(
          initial,
          Command.ChooseCollisionSelection(CollisionSelection.Deadline)
        ).scenario.collisionSelection shouldBe CollisionSelection.Deadline

      "set the collision avoidance policy" in:
        ScenarioEditor(
          initial,
          Command.ChooseCollisionAvoidance(CollisionAvoidance.Reroute)
        ).scenario.collisionAvoidance shouldBe CollisionAvoidance.Reroute

      behave like anAcceptedEdit(
        initial,
        Command.ChooseRouting(Routing.Time)
      )

    "the generator is reseeded" should:

      "set the seed of the scenario" in:
        ScenarioEditor(initial, Command.Reseed(7L)).scenario.seed shouldBe 7L
