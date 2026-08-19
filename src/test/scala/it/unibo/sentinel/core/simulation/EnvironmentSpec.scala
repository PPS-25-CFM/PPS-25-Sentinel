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

  val env = Environment(
    warehouse,
    fleet,
    board
  )

  "An Environment" when:

    "initialized" should:

      "have its parameters correctly set" in:
        
        env.placements should contain theSameElementsAs fleet.values
        env.missions should contain theSameElementsAs board.values

      
