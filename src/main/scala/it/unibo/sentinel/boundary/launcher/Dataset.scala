package it.unibo.sentinel.boundary.launcher

import it.unibo.sentinel.core.warehouse.{
  Warehouse,
  Area,
  Tile,
  Position,
  WarehouseId
}
import it.unibo.sentinel.core.mission.{Mission, MissionId}
import it.unibo.sentinel.core.robot.RobotId
import it.unibo.sentinel.core.scenario.Scenario
import it.unibo.sentinel.core.scenario.Spawn
import it.unibo.sentinel.core.simulation.Tick
import it.unibo.sentinel.core.scenario.ScenarioId
import it.unibo.sentinel.core.scenario.Policies.CollisionSelection
import it.unibo.sentinel.core.mission.Priority

/** Contains default values for a test simulation
  */
object Dataset:

  /** Width of the warehouse
    */
  val width: Int = 20

  /** Height of the warehouse
    */
  val height: Int = 20

  /** @return
    *   a [[Warehouse]] of size `width x height` with a ring of non-traversable
    *   tiles all around
    */
  def warehouse: Warehouse =
    val area: Area =
      Area(Position(1, 1), Position(width - 2, height - 2))
    Warehouse
      .empty(WarehouseId("w01"), width, height)
      .withArea(area):
        Tile.Floor()

  def scenario: Scenario =
    (for
      s0 <- Right(Scenario.in(warehouse))
      s1 <- s0.place(Spawn(RobotId("R1"), Position(1, 6)))
      s2 <- s1.place(Spawn(RobotId("R2"), Position(6, 1)))
      s3 <- s2.place(Spawn(RobotId("R3"), Position(11, 6)))
      s4 <- s3.load(
        Mission
          .relocate(MissionId("M1"), Position(6, 6), Tick(10), Priority.normal)
      )
      s5 <- s4.load(
        Mission
          .relocate(MissionId("M2"), Position(6, 6), Tick(9), Priority.lowest)
      )
      s6 <- s5.load(
        Mission
          .relocate(MissionId("M3"), Position(6, 6), Tick(8), Priority.highest)
      )
      s7 <- s6.load(
        Mission
          .relocate(MissionId("M4"), Position(18, 1), Tick(20), Priority.normal)
      )
      s8 <- s7.load(
        Mission
          .relocate(MissionId("M5"), Position(1, 18), Tick(20), Priority.normal)
      )
    yield s8.withCollisionSelection(CollisionSelection.Priority)) match
      case Left(_)      => sys.exit(1)
      case Right(value) => value.withId(ScenarioId("s01"))
