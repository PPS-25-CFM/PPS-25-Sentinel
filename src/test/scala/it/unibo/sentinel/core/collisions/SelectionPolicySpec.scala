package it.unibo.sentinel.core.collisions

import it.unibo.sentinel.UnitTest
import it.unibo.sentinel.core.mission.{Mission, MissionId, Priority}
import it.unibo.sentinel.core.robot.RobotId
import it.unibo.sentinel.core.simulation.Tick
import it.unibo.sentinel.core.warehouse.Position
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import scala.util.Random
import it.unibo.sentinel.core.robot.RobotStatus

trait SelectionPolicyFixture:
  self: UnitTest =>

  val count = 5
  val missions: Seq[Mission] = (1 to count).map { i =>
    Mission.relocate(
      MissionId(s"M$i"),
      Position(i, i),
      Tick(i),
      Priority(i)
    )
  }
  val intents: Seq[Intent] = missions.zipWithIndex.map { case (mission, idx) =>
    val id = idx + 1
    Intent(
      RobotId(s"R$id"),
      Position(0, idx),
      Position(idx, idx),
      Some(mission),
      RobotStatus.Moving
    )
  }

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
