package it.unibo.sentinel.boundary.launcher

import it.unibo.sentinel.core.warehouse.{Warehouse, Area, Tile, Position}
import it.unibo.sentinel.core.mission.{Mission, MissionId, Task}
import it.unibo.sentinel.core.robot.RobotId
import it.unibo.sentinel.core.scenario.Scenario
import it.unibo.sentinel.core.scenario.Spawn
import it.unibo.sentinel.core.scenario.Validation

/** Contains default values for a test simulation
  */
trait Dataset:

  /** Width of the warehouse
    */
  protected val width: Int = 50

  /** Height of the warehouse
    */
  protected val height: Int = 50

  protected val nRobots: Int = 1

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
    val withRobot = extractScenario:
      Scenario
        .in(warehouse)
        .place(Spawn(RobotId("R1"), Position(1, 1)))
    extractScenario:
      withRobot.load(mission)

  protected def extractScenario(
      either: Either[Validation, Scenario]
  ): Scenario =
    either match
      case Left(_)      => sys.exit(1)
      case Right(value) => value
