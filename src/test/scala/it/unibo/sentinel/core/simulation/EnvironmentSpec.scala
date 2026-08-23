package it.unibo.sentinel.core.simulation

import it.unibo.sentinel.UnitTest
import it.unibo.sentinel.core.mission.{Mission, MissionId, MissionStatus, Task}
import it.unibo.sentinel.core.robot.{Robot, RobotId, RobotStatus}
import it.unibo.sentinel.core.routing.Path
import it.unibo.sentinel.core.scenario.{Scenario, Spawn}
import it.unibo.sentinel.core.warehouse.{Area, Position, Tile, Warehouse}
import org.scalatest.BeforeAndAfterEach

import scala.compiletime.uninitialized

trait EnvironmentFixture:
  self: UnitTest =>

  val width = 5
  val r_id1: RobotId = RobotId("R1")
  val r_id2: RobotId = RobotId("R2")
  val m_id1: MissionId = MissionId("M1")
  val m_id2: MissionId = MissionId("M2")

  val pos1: Position = Position(0, 0)
  val pos2: Position = Position(2, 2)

  val warehouse: Warehouse = Warehouse
    .empty(width, width)
    .withArea(Area(Position(0, 0), Position(width - 1, width - 1)))(
      Tile.Floor()
    )

  val scenario: Scenario = (for
    s0 <- Right(Scenario.in(warehouse))
    s1 <- s0.place(Spawn(r_id1, pos1))
    s2 <- s1.place(Spawn(r_id2, pos2))
    s3 <- s2.load(Mission(m_id1, Task.goto(Position(0, 1)), 10))
    s4 <- s3.load(Mission(m_id2, Task.goto(Position(4, 4)), 10))
  yield s4).value

/* We suppressed null warning due to the ScalaTest lifecycle `uninitialized` var usage in beforeEach.
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
        val event = environment.assign(r_id1, m_id1)
        event shouldBe Some(Event.MissionAssigned(r_id1, m_id1))

        val assignedMission = environment.mission(m_id1).value
        assignedMission.carrier shouldBe Some(r_id1)
        assignedMission.status shouldBe MissionStatus.Assigned

        val assignedRobot = environment.robot(r_id1).value
        assignedRobot.status shouldBe RobotStatus.Ready
        assignedRobot.mission.value shouldBe m_id1

      "return None and do nothing if the robot ID does not exist" in:
        val initialPlacements = environment.placements
        val initialMissions = environment.missions

        val event = environment.assign(RobotId("UNKNOWN"), m_id1)
        event shouldBe None

        environment.placements shouldBe initialPlacements
        environment.missions shouldBe initialMissions

      "return None and do nothing if the mission ID does not exist" in:
        val initialPlacements = environment.placements
        val initialMissions = environment.missions

        val event = environment.assign(r_id1, MissionId("UNKNOWN"))
        event shouldBe None

        environment.placements shouldBe initialPlacements
        environment.missions shouldBe initialMissions

    "routing a robot" should:

      "assign to the robot the path and return RobotRouted if it exists in fleet" in:
        val path: Path = Seq.empty
        val event = environment.route(r_id1, path)
        event shouldBe Some(Event.RobotRouted(r_id1, path))

        val routedBot = environment.robot(r_id1).value
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
        val target = Position(0, 1)
        val path: Path = Seq(target)

        environment.route(r_id1, path)
        val event = environment.advance(r_id1)

        event shouldBe Some(Event.RobotMoved(r_id1, pos1, target))

        val updatedPlacement = environment.placement(r_id1).value
        updatedPlacement.at shouldBe target

      "prevent movement and return RobotBlocked if target position is occupied by another robot" in:
        val collisionPath: Path = Seq(pos2)

        environment.route(r_id1, collisionPath)
        val event = environment.advance(r_id1)

        event shouldBe Some(Event.RobotBlocked(r_id1, pos1))

        val placementAfterCollision = environment.placement(r_id1).value
        placementAfterCollision.at shouldBe pos1

      "advance step-by-step through a Path returning RobotMoved events" in:
        val step1 = Position(0, 1)
        val step2 = Position(0, 2)
        val path: Path = Seq(step1, step2)

        environment.route(r_id1, path)

        environment.advance(r_id1) shouldBe Some(
          Event.RobotMoved(r_id1, pos1, step1)
        )
        environment.placement(r_id1).value.at shouldBe step1

        environment.advance(r_id1) shouldBe Some(
          Event.RobotMoved(r_id1, step1, step2)
        )
        environment.placement(r_id1).value.at shouldBe step2

    "performing a mission" should:

      "complete the mission, release the robot, and return MissionCompleted when assigned" in:
        environment.assign(r_id1, m_id1)

        val event = environment.perform(r_id1)
        event shouldBe Some(Event.MissionCompleted(m_id1))

        environment.mission(m_id1).value.status shouldBe MissionStatus.Completed
        environment.robot(r_id1).value.mission shouldBe None

      "return None if the robot ID does not exist" in:
        val event = environment.perform(RobotId("UNKNOWN"))
        event shouldBe None

      "return None if the robot has no mission assigned" in:
        val event = environment.perform(r_id1)
        event shouldBe None

    "ticking simulation time" should:

      "advance all missions and return empty events when no mission fails" in:
        val events = environment.tick()
        events shouldBe empty

      "return MissionFailed events when a mission expires or transitions to Failed status" in:
        // Avanza il tempo oltre la durata della missione (10 tick) per provocare il fallimento
        val events = (1 to 11).flatMap(_ => environment.tick())

        events should contain(Event.MissionFailed(m_id1))
        events should contain(Event.MissionFailed(m_id2))
        environment.mission(m_id1).value.status shouldBe MissionStatus.Failed
        environment.mission(m_id2).value.status shouldBe MissionStatus.Failed

    "queried about Standings" should:

      "return Placements matching a specific Robot status" in:
        environment
          .standing(RobotStatus.Idle)
          .map(_.robot.id) should contain theSameElementsAs Seq(r_id1, r_id2)
        environment.standing(RobotStatus.Ready) shouldBe empty

        environment.assign(r_id1, m_id1)

        environment
          .standing(RobotStatus.Ready)
          .map(_.robot.id) should contain theSameElementsAs Seq(r_id1)
        environment
          .standing(RobotStatus.Idle)
          .map(_.robot.id) should contain theSameElementsAs Seq(r_id2)

    "queried about Pending Missions" should:

      "return only missions in Pending status with pendingMissions" in:
        environment.pendingMissions.map(
          _.id
        ) should contain theSameElementsAs Seq(m_id1, m_id2)

        environment.assign(r_id1, m_id1)

        environment.pendingMissions.map(
          _.id
        ) should contain theSameElementsAs Seq(m_id2)

    "querying entities by ID" should:

      "return the robot if it exists" in:
        environment.robot(r_id1).value.id shouldBe r_id1
        environment.robot(RobotId("UNKNOWN")) shouldBe None

      "return the mission if it exists" in:
        environment.mission(m_id1).value.id shouldBe m_id1
        environment.mission(MissionId("UNKNOWN")) shouldBe None

      "return the placement if it exists" in:
        val place = environment.placement(r_id1).value
        place.robot.id shouldBe r_id1
        place.at shouldBe pos1

        environment.placement(RobotId("UNKNOWN")) shouldBe None
