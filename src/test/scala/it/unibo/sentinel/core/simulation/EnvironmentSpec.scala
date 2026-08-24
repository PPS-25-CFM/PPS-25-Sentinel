package it.unibo.sentinel.core.simulation

import it.unibo.sentinel.UnitTest
import it.unibo.sentinel.core.mission.{MissionId, MissionStatus}
import it.unibo.sentinel.core.robot.{RobotId, RobotStatus}
import it.unibo.sentinel.core.routing.Path
import it.unibo.sentinel.core.warehouse.Position
import org.scalatest.BeforeAndAfterEach

import scala.compiletime.uninitialized
import it.unibo.sentinel.core.mission.{Mission, Task}

/*
 * We suppressed null warning due to the ScalaTest lifecycle `uninitialized` var usage in beforeEach.
 */
@SuppressWarnings(Array("org.wartremover.warts.Null"))
class EnvironmentSpec
    extends UnitTest
    with BeforeAndAfterEach
    with EnvironmentFixture:

  var environment: Environment = uninitialized

  override def beforeEach(): Unit =
    environment = scenario.build

  "An Environment" when:

    "assigning a mission" should:

      "update the robot and board and return MissionAssigned when IDs exist" in:
        val event = environment.assign(r1, m1)
        event shouldBe Some(Event.MissionAssigned(r1, m1))

        val assignedMission = environment.mission(m1).value
        assignedMission.carrier shouldBe Some(r1)
        assignedMission.status shouldBe MissionStatus.Assigned

        val assignedRobot = environment.robot(r1).value
        assignedRobot.status shouldBe RobotStatus.Ready
        assignedRobot.mission.value shouldBe m1

      "return None and do nothing if the robot ID does not exist" in:
        val initialPlacements = environment.placements
        val initialMissions = environment.missions

        val event = environment.assign(RobotId("UNKNOWN"), m1)
        event shouldBe None

        environment.placements shouldBe initialPlacements
        environment.missions shouldBe initialMissions

      "return None and do nothing if the mission ID does not exist" in:
        val initialPlacements = environment.placements
        val initialMissions = environment.missions

        val event = environment.assign(r1, MissionId("UNKNOWN"))
        event shouldBe None

        environment.placements shouldBe initialPlacements
        environment.missions shouldBe initialMissions

    "routing a robot" should:

      "assign to the robot the path and return RobotRouted if it exists in fleet" in:
        val path: Path = Seq.empty
        val event = environment.route(r1, path)
        event shouldBe Some(Event.RobotRouted(r1, path))

        val routedBot = environment.robot(r1).value
        routedBot.path shouldBe Some(path)

      "return None and do nothing if the robot ID does not exist" in:
        val initialPlacements = environment.placements
        val initialMissions = environment.missions
        val path: Path = Seq.empty

        val event = environment.route(RobotId("UNKNOWN"), path)
        event shouldBe None

        environment.placements shouldBe initialPlacements
        environment.missions shouldBe initialMissions

    "advancing a robot" should:

      "update placement, step the robot and return RobotMoved if target position is free" in:
        val target = Position(1, 2)
        val path: Path = Seq(target)

        environment.route(r1, path)
        val event = environment.advance(r1)

        event shouldBe Some(Event.RobotMoved(r1, p1, target))

        val updatedPlacement = environment.placement(r1).value
        updatedPlacement.at shouldBe target

      "prevent movement and return RobotBlocked if target position is occupied by another robot" in:
        val collisionPath: Path = Seq(p2)

        environment.route(r1, collisionPath)
        val event = environment.advance(r1)

        event shouldBe Some(Event.RobotBlocked(r1, p1))

        val placementAfterCollision = environment.placement(r1).value
        placementAfterCollision.at shouldBe p1

      "advance step-by-step through a Path returning RobotMoved events" in:
        val step1 = Position(1, 2)
        val step2 = Position(1, 3)
        val path: Path = Seq(step1, step2)

        environment.route(r1, path)

        environment.advance(r1) shouldBe Some(
          Event.RobotMoved(r1, p1, step1)
        )
        environment.placement(r1).value.at shouldBe step1

        environment.advance(r1) shouldBe Some(
          Event.RobotMoved(r1, step1, step2)
        )
        environment.placement(r1).value.at shouldBe step2

    "performing a mission" should:

      "complete the mission, release the robot, and return MissionCompleted when assigned" in:
        environment.assign(r1, m1)

        val event = environment.perform(r1)
        event shouldBe Some(Event.MissionCompleted(m1))

        environment.mission(m1).value.status shouldBe MissionStatus.Completed
        environment.robot(r1).value.mission shouldBe None

      "return None if the robot ID does not exist" in:
        val event = environment.perform(RobotId("UNKNOWN"))
        event shouldBe None

      "return None if the robot has no mission assigned" in:
        val event = environment.perform(r1)
        event shouldBe None

    "ticking simulation time" should:

      "advance all missions and return empty events when no mission fails" in:
        val events = environment.tick()
        events shouldBe empty

      "return MissionFailed events and release assigned carriers when missions fail" in:
        val uncopletable = Mission(MissionId("Uncompletable"), Task.goto(p4), 1)
        val uncScenario = scenario.load(uncopletable).right.value
        environment = uncScenario.build

        environment.assign(r1, uncopletable.id)
        val events = environment.tick()

        events should contain(Event.MissionFailed(uncopletable.id))

        val failed = environment.mission(uncopletable.id).value

        failed.status shouldBe MissionStatus.Failed
        failed.carrier shouldBe None

        val robot = environment.robot(r1).value
        robot.status shouldBe RobotStatus.Idle
        robot.mission shouldBe None

    "queried about Standings" should:

      "return Placements matching a specific Robot status" in:
        environment
          .standing(RobotStatus.Idle)
          .map(_.robot.id) should contain theSameElementsAs Seq(r1, r2)
        environment.standing(RobotStatus.Ready) shouldBe empty

        environment.assign(r1, m1)

        environment
          .standing(RobotStatus.Ready)
          .map(_.robot.id) should contain theSameElementsAs Seq(r1)
        environment
          .standing(RobotStatus.Idle)
          .map(_.robot.id) should contain theSameElementsAs Seq(r2)

    "queried about Pending Missions" should:

      "return only missions in Pending status with pendingMissions" in:
        environment.pendingMissions.map(
          _.id
        ) should contain theSameElementsAs Seq(m1, m2)

        environment.assign(r1, m1)

        environment.pendingMissions.map(
          _.id
        ) should contain theSameElementsAs Seq(m2)

    "querying entities by ID" should:

      "return the robot if it exists" in:
        environment.robot(r1).value.id shouldBe r1
        environment.robot(RobotId("UNKNOWN")) shouldBe None

      "return the mission if it exists" in:
        environment.mission(m1).value.id shouldBe m1
        environment.mission(MissionId("UNKNOWN")) shouldBe None

      "return the placement if it exists" in:
        val place = environment.placement(r1).value
        place.robot.id shouldBe r1
        place.at shouldBe p1

        environment.placement(RobotId("UNKNOWN")) shouldBe None
