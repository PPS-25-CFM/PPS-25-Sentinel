package it.unibo.sentinel.core.simulation

import it.unibo.sentinel.UnitTest
import it.unibo.sentinel.core.warehouse.Warehouse
import it.unibo.sentinel.core.warehouse.Position
import it.unibo.sentinel.core.robot.Robot
import it.unibo.sentinel.core.robot.RobotId
import it.unibo.sentinel.core.scenario.Placement
import it.unibo.sentinel.core.mission.MissionId
import it.unibo.sentinel.core.mission.Mission
import it.unibo.sentinel.core.mission.Task
import it.unibo.sentinel.core.mission.MissionStatus
import it.unibo.sentinel.core.robot.RobotStatus

class EnvironmentSpec extends UnitTest:

  val pos1 = Position(1, 1)
  val pos2 = Position(2, 2)

  val r_id1 = RobotId("R1")
  val r_id2 = RobotId("R2")

  val bot1 = Robot(r_id1)
  val bot2 = Robot(r_id2)

  val place1 = Placement(bot1, pos1)
  val place2 = Placement(bot2, pos2)

  val des1 = Position(3, 3)
  val des2 = Position(4, 4)

  val task1 = Task.goto(des1)
  val task2 = Task.goto(des2)

  val m_id1 = MissionId("M1")
  val m_id2 = MissionId("M2")

  val duration = 10

  val mission1 = Mission(m_id1, task1, duration)
  val mission2 = Mission(m_id2, task2, duration)

  val width = 5
  val height = width

  val warehouse = Warehouse.empty(width, height)
  val fleet = Map[RobotId, Placement](
    (r_id1, place1),
    (r_id2, place2)
  )
  val board = Map[MissionId, Mission](
    (m_id1, mission1),
    (m_id2, mission2)
  )

  "An Environment" when:

    "initialized" should:

      "have its parameters correctly set" in:
        val env = Environment(warehouse, fleet, board)

        env.warehouse shouldBe warehouse
        env.placements shouldBe fleet.values.toSeq
        env.missions shouldBe board.values.toSeq

    "assigning a mission" should:

      "update the robot and board when IDs exist" in:
        val env = Environment(warehouse, fleet, board)
        env.assign(r_id1, m_id1)

        val assignedMission = env.missions.find(_.id == m_id1).value
        
        assignedMission.carrier shouldBe Some(r_id1)
        assignedMission.status shouldBe MissionStatus.Assigned

        val assignedRobot = env.placements.map(_.robot).find(_.id == r_id1).value

        assignedRobot.status shouldBe RobotStatus.Ready
        assignedRobot.mission.value shouldBe m_id1

      "do nothing if the robot ID does not exist" in:
        val env = Environment(warehouse, fleet, board)
        env.assign(RobotId("UNKNOWN"), m_id1)

        env.missions shouldBe board.values.toSeq

      "do nothing if the mission ID does not exist" in:
        val env = Environment(warehouse, fleet, board)
        env.assign(r_id1, MissionId("UNKNOWN"))

        env.missions shouldBe board.values.toSeq
