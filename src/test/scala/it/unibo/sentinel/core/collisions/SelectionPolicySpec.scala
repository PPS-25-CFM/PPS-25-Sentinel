package it.unibo.sentinel.core.collisions

import it.unibo.sentinel.UnitTest
import it.unibo.sentinel.core.mission.{Mission, MissionId, Priority}
import it.unibo.sentinel.core.robot.RobotId
import it.unibo.sentinel.core.scenario.Intent
import it.unibo.sentinel.core.simulation.Tick
import it.unibo.sentinel.core.warehouse.Position
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import scala.util.Random

trait SelectionPolicyFixture:
  self: UnitTest =>

  val missions: Seq[Mission] = Seq(
    Mission.relocate(MissionId("M1"), Position(1, 1), Tick(1), Priority(1)),
    Mission.relocate(MissionId("M2"), Position(2, 2), Tick(2), Priority(2)),
    Mission.relocate(MissionId("M3"), Position(3, 3), Tick(3), Priority(3)),
    Mission.relocate(MissionId("M4"), Position(4, 4), Tick(4), Priority(4)),
    Mission.relocate(MissionId("M5"), Position(5, 5), Tick(5), Priority(5))
  )

  val intents: Seq[Intent] = Seq(
    Intent(RobotId("R1"), Position(0, 0), Position(1, 1), Some(missions(0))),
    Intent(RobotId("R2"), Position(0, 1), Position(2, 2), Some(missions(1))),
    Intent(RobotId("R3"), Position(0, 2), Position(3, 3), Some(missions(2))),
    Intent(RobotId("R4"), Position(0, 3), Position(4, 4), Some(missions(3))),
    Intent(RobotId("R5"), Position(0, 4), Position(5, 5), Some(missions(4)))
  )

class SelectionPolicySpec
    extends UnitTest
    with SelectionPolicyFixture
    with MockitoSugar:

  private def rigged(
      rng: Random,
      order: Seq[RobotId]
  ): Unit =
    when(rng.shuffle(any[Seq[RobotId]]())(using any())).thenReturn(order)

  "A selection policy" when:

    "selecting randomly" should:

      "return None when no intents are given" in:
        val rng = mock[Random]
        rigged(rng, Seq.empty)
        SelectionPolicy.random(rng).select(Seq.empty) shouldBe None

      "select the head of the order produced by the RNG" in:
        val ids = intents.map(_.robotId)
        val order = Seq(ids(2), ids(0), ids(1), ids(3), ids(4))
        val rng = mock[Random]
        rigged(rng, order)
        SelectionPolicy.random(rng).select(intents) shouldBe Some(ids(2))

      "return the only id when a single intent is given" in:
        val id =
          intents.headOption.map(_.robotId).getOrElse(fail("intents is empty"))
        val rng = mock[Random]
        rigged(rng, Seq(id))
        SelectionPolicy.random(rng).select(intents.take(1)) shouldBe Some(id)

    "selecting based on mission deadline" should:
      val policy = SelectionPolicy.closestDeadline()

      "select the robot with the mission closest to failing" in:
        policy.select(intents) shouldBe intents.headOption.map(_.robotId)

    "selecting based on mission priority" should:
      val policy = SelectionPolicy.highestPriority()

      "select the robot with the highest priority mission" in:
        policy.select(intents) shouldBe intents.lastOption.map(_.robotId)
