package it.unibo.sentinel.core.collisions

import it.unibo.sentinel.UnitTest
import it.unibo.sentinel.core.robot.RobotId
import it.unibo.sentinel.core.robot.Robot
import it.unibo.sentinel.core.mission.Mission
import it.unibo.sentinel.core.mission.MissionId
import it.unibo.sentinel.core.warehouse.Position
import it.unibo.sentinel.core.simulation.Tick
import it.unibo.sentinel.core.mission.Priority
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import scala.collection.generic.CanBuildFrom
import scala.util.Random

trait SelectionPolicyFixture:
  self: UnitTest =>

  val robots: Seq[Robot] = Seq(
    Robot.drone(RobotId("R1")),
    Robot.drone(RobotId("R2")),
    Robot.drone(RobotId("R3")),
    Robot.drone(RobotId("R4")),
    Robot.drone(RobotId("R5"))
  )
  val missions: Seq[Mission] = Seq(
    Mission.relocate(MissionId("M1"), Position(1, 1), Tick(1), Priority(1)),
    Mission.relocate(MissionId("M2"), Position(2, 2), Tick(2), Priority(2)),
    Mission.relocate(MissionId("M3"), Position(3, 3), Tick(3), Priority(3)),
    Mission.relocate(MissionId("M4"), Position(4, 4), Tick(4), Priority(4)),
    Mission.relocate(MissionId("M5"), Position(5, 5), Tick(5), Priority(5))
  )
  for
    i <- 0 until 5
    robot = robots(i)
    mission = missions(i)
  do robot.accept(mission)

class SelectionPolicySpec
    extends UnitTest
    with SelectionPolicyFixture
    with MockitoSugar:

  private type IdOrder = CanBuildFrom[Seq[RobotId], RobotId, Seq[RobotId]]

  private def rigged(
      rng: Random,
      ids: Seq[RobotId],
      order: Seq[RobotId]
  ): Unit =
    when(rng.shuffle(eqTo(ids))(using any[IdOrder]())).thenReturn(order)

  "A selection policy" when:
    given Seq[Mission] = missions

    "selecting randomly" should:

      "return None when no robots are given" in:
        val rng = mock[Random]
        rigged(rng, Seq.empty, Seq.empty)

        SelectionPolicy.random(rng).select(Seq.empty) shouldBe None

      "select the head of the order produced by the RNG" in:
        val ids = robots.map(_.id)
        val order = Seq(ids(2), ids(0), ids(1), ids(3), ids(4))
        val rng = mock[Random]
        rigged(rng, ids, order)

        SelectionPolicy.random(rng).select(robots) shouldBe Some(ids(2))

      "return the only id when a single robot is given" in:
        val id = robots(0).id
        val rng = mock[Random]
        rigged(rng, Seq(id), Seq(id))

        SelectionPolicy.random(rng).select(robots.take(1)) shouldBe Some(id)

    "selecting based on mission deadline" should:
      val policy = SelectionPolicy.closestDeadline()

      "select the robot with the mission closest to failing" in:
        policy.select(robots) shouldBe Some(robots(0).id)

    "selecting based on mission priority" should:
      val policy = SelectionPolicy.highestPriority()

      "select the robot with the highest priority mission" in:
        policy.select(robots) shouldBe robots.lastOption.map(_.id)
