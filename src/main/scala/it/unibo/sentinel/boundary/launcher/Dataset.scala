package it.unibo.sentinel.boundary.launcher

import it.unibo.sentinel.core.warehouse.{Warehouse, Area, Tile, Position}
import it.unibo.sentinel.core.mission.{Mission, MissionId}
import it.unibo.sentinel.core.robot.RobotId
import it.unibo.sentinel.core.scenario.Scenario
import it.unibo.sentinel.core.scenario.Spawn
import it.unibo.sentinel.core.simulation.Tick

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

  protected def scenario: Scenario =
    (for
      s0 <- Right(Scenario.in(warehouse))
      s1 <- s0.place(Spawn(RobotId("R1"), Position(1, 6)))
      s2 <- s1.place(Spawn(RobotId("R2"), Position(6, 1)))
      s3 <- s2.place(Spawn(RobotId("R3"), Position(11, 6)))
      s4 <- s3.load(Mission.relocate(MissionId("M1"), Position(6, 6), Tick(10)))
      s5 <- s4.load(Mission.relocate(MissionId("M2"), Position(6, 6), Tick(10)))
      s6 <- s5.load(Mission.relocate(MissionId("M3"), Position(6, 6), Tick(10)))
      s7 <- s6.load(
        Mission.relocate(MissionId("M4"), Position(18, 1), Tick(20))
      )
      s8 <- s7.load(
        Mission.relocate(MissionId("M5"), Position(1, 18), Tick(20))
      )
    yield s8) match
      case Left(_)      => sys.exit(1)
      case Right(value) => value
