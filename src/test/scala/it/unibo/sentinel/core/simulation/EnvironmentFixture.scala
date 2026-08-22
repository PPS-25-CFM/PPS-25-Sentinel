package it.unibo.sentinel.core.simulation

import it.unibo.sentinel.UnitTest
import it.unibo.sentinel.core.TestData
import it.unibo.sentinel.core.mission.{Mission, MissionId, Task}
import it.unibo.sentinel.core.robot.RobotId
import it.unibo.sentinel.core.scenario.{Scenario, Spawn}
import it.unibo.sentinel.core.warehouse.Position

/** A mix in that contains a scenario with two robots and two missions.
  *
  * Every position lies inside the floored room of [[TestData]], and the
  * distances are chosen so that the nearest robot to each mission is unique.
  */
trait EnvironmentFixture extends TestData:
  self: UnitTest =>

  val r1: RobotId = RobotId("R1")
  val r2: RobotId = RobotId("R2")
  val m1: MissionId = MissionId("M1")
  val m2: MissionId = MissionId("M2")

  val p1: Position = Position(1, 1)
  val p2: Position = Position(3, 1)

  val p3: Position = Position(1, 2)
  val p4: Position = Position(3, 3)

  val deadline = 10

  val scenario: Scenario = (for
    s0 <- Right(emptyScenario)
    s1 <- s0.place(Spawn(r1, p1))
    s2 <- s1.place(Spawn(r2, p2))
    s3 <- s2.load(Mission(m1, Task.goto(p3), deadline))
    s4 <- s3.load(Mission(m2, Task.goto(p4), deadline))
  yield s4).value
