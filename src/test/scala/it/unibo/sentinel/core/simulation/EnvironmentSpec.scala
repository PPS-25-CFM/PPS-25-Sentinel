package it.unibo.sentinel.core.simulation

import it.unibo.sentinel.UnitTest
import it.unibo.sentinel.core.mission.{Mission, MissionId, MissionStatus, Task}
import it.unibo.sentinel.core.robot.{RobotId, RobotStatus}
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

/** We suppressed null warning due to the ScalaTest lifecycle `uninitialized` var usage in beforeEach.
  */
@SuppressWarnings(Array("org.wartremover.warts.Null"))
class EnvironmentSpec
    extends UnitTest
    with BeforeAndAfterEach
    with EnvironmentFixture:

  var environment: Environment = uninitialized

  override def beforeEach(): Unit =
    super.beforeEach()
    environment = scenario.build

  "An Environment" when:

    "initialized" should:

      "have its parameters correctly set" in:
        environment.warehouse shouldBe warehouse
        environment.placements.map(_.robot.id) should contain theSameElementsAs Seq(r_id1, r_id2)
        environment.missions.map(_.id) should contain theSameElementsAs Seq(m_id1, m_id2)

    "assigning a mission" should:

      "update the robot and board when IDs exist" in:
        environment.assign(r_id1, m_id1)

        val assignedMission = environment.missions.find(_.id == m_id1).value
        assignedMission.carrier shouldBe Some(r_id1)
        assignedMission.status shouldBe MissionStatus.Assigned

        val assignedRobot = environment.placements.map(_.robot).find(_.id == r_id1).value
        assignedRobot.status shouldBe RobotStatus.Ready
        assignedRobot.mission.value shouldBe m_id1

      "do nothing if the robot ID does not exist" in:
        val initialPlacements = environment.placements
        val initialMissions = environment.missions

        environment.assign(RobotId("UNKNOWN"), m_id1)

        environment.placements shouldBe initialPlacements
        environment.missions shouldBe initialMissions

      "do nothing if the mission ID does not exist" in:
        val initialPlacements = environment.placements
        val initialMissions = environment.missions

        environment.assign(r_id1, MissionId("UNKNOWN"))

        environment.placements shouldBe initialPlacements
        environment.missions shouldBe initialMissions

    "routing a robot" should:

      "assign to the robot the path, if it exists in fleet" in:
        val path: Path = Seq.empty
        environment.route(r_id1, path)

        val routedBot = environment.placements.find(_.robot.id == r_id1).value.robot
        routedBot.path shouldBe Some(path)

      "do nothing if the robot ID does not exist" in:
        val initialPlacements = environment.placements
        val initialMissions = environment.missions
        val path: Path = Seq.empty

        environment.route(RobotId("UNKNOWN"), path)

        environment.placements shouldBe initialPlacements
        environment.missions shouldBe initialMissions

    "advancing a robot" should:

      "update placement and step the robot if target position is free" in:
        val target = Position(0, 1)
        val path: Path = Seq(target)

        environment.route(r_id1, path)
        environment.advance(r_id1)

        val updatedPlacement = environment.placements.find(_.robot.id == r_id1).value
        updatedPlacement.at shouldBe target

      "prevent movement if the target position is occupied by another robot" in:
        // pos2 is occupied by r_id2 at Position(2, 2)
        val collisionPath: Path = Seq(pos2)

        environment.route(r_id1, collisionPath)
        environment.advance(r_id1)

        val placementAfterCollision = environment.placements.find(_.robot.id == r_id1).value
        placementAfterCollision.at shouldBe pos1

      "advance step-by-step through a Path" in:
        val step1 = Position(0, 1)
        val step2 = Position(0, 2)
        val path: Path = Seq(step1, step2)

        environment.route(r_id1, path)

        environment.advance(r_id1)
        environment.placements.find(_.robot.id == r_id1).value.at shouldBe step1

        environment.advance(r_id1)
        environment.placements.find(_.robot.id == r_id1).value.at shouldBe step2

    "queried about Standings" should:

      "return Placements matching a specific Robot status" in:
        environment.standing(RobotStatus.Idle).map(_.robot.id) should contain theSameElementsAs Seq(r_id1, r_id2)
        environment.standing(RobotStatus.Ready) shouldBe empty

        environment.assign(r_id1, m_id1)

        environment.standing(RobotStatus.Ready).map(_.robot.id) should contain theSameElementsAs Seq(r_id1)
        environment.standing(RobotStatus.Idle).map(_.robot.id) should contain theSameElementsAs Seq(r_id2)

    "queried about Pending Missions" should:

      "return only missions in Pending status with pendingMissions" in:
        environment.pendingMissions.map(_.id) should contain theSameElementsAs Seq(m_id1, m_id2)

        environment.assign(r_id1, m_id1)

        environment.pendingMissions.map(_.id) should contain theSameElementsAs Seq(m_id2)