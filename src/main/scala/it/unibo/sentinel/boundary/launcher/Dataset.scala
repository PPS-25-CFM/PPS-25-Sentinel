package it.unibo.sentinel.boundary.launcher

import it.unibo.sentinel.core.warehouse.Warehouse
import it.unibo.sentinel.core.warehouse.Area
import it.unibo.sentinel.core.warehouse.Tile
import it.unibo.sentinel.core.warehouse.Position
import it.unibo.sentinel.core.mission.Mission
import it.unibo.sentinel.core.mission.MissionId
import it.unibo.sentinel.core.mission.Task
import it.unibo.sentinel.core.robot.RobotId

/** Contains default values for a test simulation
  */
trait Dataset:

  /** Width of the warehouse
    */
  protected val width: Int = 50

  /** Height of the warehouse
    */
  protected val height: Int = 50

  /** Number of missions to create for each [[MissionStatus]]
    */
  protected val missionsPerStatus: Int = 5

  /** @return
    *   a [[Warehouse]] of size `width x height` with a ring of non-traversable
    *   tiles all around
    */
  def warehouse: Warehouse =
    val area: Area =
      Area(Position(1, 1), Position(width - 2, height - 2))
    Warehouse
      .empty(width, height)
      .withArea(area):
        Tile.Floor()

  /** @return
    *   a list of [[Mission]]s with each [[MissionStatus]]
    */
  def missions: Iterable[Mission] =
    val pending = createMissions(0)
    val assigned = createMissions(1).map(_.assignTo(RobotId("R1")))
    val completed = createMissions(2).map(_.complete)
    val failed = createMissions(3).map(_.fail)
    pending.concat(assigned).concat(completed).concat(failed)

  /** @param idOffset
    *   offset to use for calculating the missions' ids
    * @return
    *   a list of missions with size `missionsPerStatus`
    */
  private def createMissions(idOffset: Int): Iterable[Mission] =
    for i <- 0 until missionsPerStatus
    yield Mission(
      MissionId(s"M${i + idOffset * missionsPerStatus}"),
      Task.Move(Position(5, 5)),
      10
    )
