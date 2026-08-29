package it.unibo.sentinel.boundary.launcher

import it.unibo.sentinel.core.warehouse.{Warehouse, Area, Tile, Position}
import it.unibo.sentinel.core.mission.{Mission, MissionId, Task}
import it.unibo.sentinel.core.robot.RobotId
import it.unibo.sentinel.core.scenario.Scenario
import it.unibo.sentinel.core.scenario.Spawn

/** Contains default values for a test simulation
  */
trait Dataset:

  /** Width of the warehouse
    */
  protected val width: Int = 20

  /** Height of the warehouse
    */
  protected val height: Int = 20

  /** @return
    *   a [[Warehouse]] of size `width x height` with a ring of non-traversable
    *   tiles all around
    */
  protected def warehouse: Warehouse =
    val area: Area =
      Area(Position(1, 1), Position(width - 2, height - 2))
    Warehouse
      .empty(width, height)
      .withArea(area):
        Tile.Floor()

  /** @return
    *   a list of [[Mission]]s with each [[MissionStatus]]
    */
  protected def mission: Mission =
    Mission(MissionId("M1"), Task.goto(Position(5, 5)), 10)

  protected def scenario: Scenario =
    (for
      s0 <- Right(Scenario.in(warehouse))
      s1 <- s0.place(Spawn(RobotId("R1"), Position(1, 6)))
      s2 <- s1.place(Spawn(RobotId("R2"), Position(6, 1)))
      s3 <- s2.place(Spawn(RobotId("R3"), Position(11, 6)))
      s4 <- s3.load(Mission(MissionId("M1"), Task.goto(Position(6, 6)), 10))
      s5 <- s4.load(Mission(MissionId("M2"), Task.goto(Position(6, 6)), 10))
      s6 <- s5.load(Mission(MissionId("M3"), Task.goto(Position(6, 6)), 10))
      s7 <- s6.load(Mission(MissionId("M4"), Task.goto(Position(18, 1)), 20))
      s8 <- s7.load(Mission(MissionId("M5"), Task.goto(Position(1, 18)), 20))
    yield s8) match
      case Left(_)      => sys.exit(1)
      case Right(value) => value
