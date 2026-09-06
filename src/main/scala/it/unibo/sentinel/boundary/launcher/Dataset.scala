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
import it.unibo.sentinel.core.scenario.RobotClass
import it.unibo.sentinel.core.simulation.Tick
import it.unibo.sentinel.core.scenario.ScenarioId
import it.unibo.sentinel.core.item.Item

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
    *   tiles all around, plus a few [[Tile.Shelf]] and [[Tile.LoadingBay]]
    *   tiles to exercise pick & drop missions.
    */
  def warehouse: Warehouse =
    val area: Area =
      Area(Position(1, 1), Position(width - 2, height - 2))
    Warehouse
      .empty(WarehouseId("w01"), width, height)
      .withArea(area):
        Tile.Floor()
      .withTile(Position(5, 5))(Tile.Shelf(Item.Computer))
      .withTile(Position(5, 12))(Tile.Shelf(Item.Table))
      .withTile(Position(14, 8))(Tile.Shelf(Item.Fridge))
      .withTile(Position(10, 10))(Tile.LoadingBay())
      .withTile(Position(10, 12))(Tile.LoadingBay())
      .withTile(Position(14, 14))(Tile.LoadingBay())

  /** @return
    *   a demo [[Scenario]].
    */
  def scenario: Scenario =
    (for
      s0 <- Right(Scenario.in(warehouse))
      s1 <- s0.place(Spawn(RobotId("R1"), Position(1, 6), RobotClass.Drone))
      s2 <- s1.place(Spawn(RobotId("R2"), Position(6, 1), RobotClass.Drone))
      s3 <- s2.place(Spawn(RobotId("R3"), Position(11, 6), RobotClass.Drone))
      s4 <- s3.place(Spawn(RobotId("R4"), Position(1, 1), RobotClass.Carrier))
      s5 <- s4.place(
        Spawn(RobotId("R5"), Position(18, 18), RobotClass.HeavyCarrier)
      )
      s6 <- s5.load(Mission.relocate(MissionId("M1"), Position(6, 6), Tick(10)))
      s7 <- s6.load(Mission.relocate(MissionId("M2"), Position(6, 6), Tick(10)))
      s8 <- s7.load(Mission.relocate(MissionId("M3"), Position(6, 6), Tick(10)))
      s9 <- s8.load(
        Mission.relocate(MissionId("M4"), Position(18, 1), Tick(20))
      )
      s10 <- s9.load(
        Mission.relocate(MissionId("M5"), Position(1, 18), Tick(20))
      )
      s11 <- s10.load(
        Mission.deliver(
          MissionId("D1"),
          Item.Computer,
          Position(5, 5),
          Position(10, 10),
          Tick(60)
        )
      )
      s12 <- s11.load(
        Mission.deliver(
          MissionId("D2"),
          Item.Table,
          Position(5, 12),
          Position(10, 12),
          Tick(60)
        )
      )
      s13 <- s12.load(
        Mission.deliver(
          MissionId("D3"),
          Item.Fridge,
          Position(14, 8),
          Position(14, 14),
          Tick(80)
        )
      )
    yield s13) match
      case Left(_)      => sys.exit(1)
      case Right(value) => value.withId(ScenarioId("s01"))
