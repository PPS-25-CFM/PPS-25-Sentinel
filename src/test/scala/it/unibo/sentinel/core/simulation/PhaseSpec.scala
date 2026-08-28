package it.unibo.sentinel.core.simulation

import it.unibo.sentinel.UnitTest
import it.unibo.sentinel.core.assignment.Selector
import it.unibo.sentinel.core.routing.Navigator
import it.unibo.sentinel.core.warehouse.Warehouse
import org.scalatest.BeforeAndAfterEach

import scala.compiletime.uninitialized
import it.unibo.sentinel.core.warehouse.Position
import it.unibo.sentinel.core.mission.MissionStatus
import it.unibo.sentinel.core.robot.RobotStatus
import it.unibo.sentinel.core.collisions.SelectionPolicy
import it.unibo.sentinel.core.collisions.CollisionHandler
import it.unibo.sentinel.core.routing.Path
import it.unibo.sentinel.core.routing.Step

class PhaseSpec
    extends UnitTest
    with BeforeAndAfterEach
    with EnvironmentFixture:

  given Warehouse = warehouse
  given navigator: Navigator = scenario.routing()
  given selector: Selector = scenario.assignment()
  given SelectionPolicy = scenario.collisionSelection()
  given CollisionHandler = scenario.collisionAvoidance()

  /*
   * We suppressed null warning due to the ScalaTest lifecycle `uninitialized` var usage in beforeEach.
   */
  @SuppressWarnings(Array("org.wartremover.warts.Null"))
  var world: Environment = uninitialized

  override def beforeEach(): Unit =
    world = scenario.build

  "The assigning phase" when:

    "there are pending missions" should:

      "assign each pending mission to the nearest available robot" in:
        Phase.assigning(world) should contain theSameElementsAs Seq(
          Event.MissionAssigned(r1, m1),
          Event.MissionAssigned(r2, m2)
        )

    "there are no pending missions" should:

      "do nothing" in:
        val _ = Phase.assigning(world)
        Phase.assigning(world) shouldBe empty

    "there are no available robots" should:

      "not assign any mission" in:
        val _ = world.assign(r1, m1)
        val _ = world.assign(r2, m2)

        Phase.assigning(world) shouldBe empty

  "The routing phase" when:

    "there are ready robots" should:

      "route each ready robot to its destination" in:
        Phase.assigning(world)

        Phase.routing(world) should contain theSameElementsAs Seq(
          Event.RobotRouted(r1, Seq(p3)),
          Event.RobotRouted(r2, Seq(Position(3, 2), p4))
        )

    "there are no ready robots" should:

      "do nothing" in:
        Phase.routing(world) shouldBe empty

  "The handle collisions phase" when:

    "there are colliding robots" should:

      "pause some robots and let another proceed" in:
        Phase.assigning(world)
        world.route(r1, Path(Step(p3, Tick.unit), Step(p4, Tick.unit)))
        world.route(r2, Path(Step(p3, Tick.unit), Step(p4, Tick.unit)))
        Phase.collisionHandling(world) should matchPattern {
          case Seq(Event.RobotBlocked(_, _)) =>
        }

  "The moving phase" when:

    "robots are routed" should:

      "advance every one of them by one position" in:
        Phase.assigning(world)
        Phase.routing(world)
        Phase.expiring(world)
        Phase.moving(world) should contain theSameElementsAs Seq(
          Event.RobotMoved(r1, from = p1, to = p3),
          Event.RobotMoved(r2, from = p2, to = Position(3, 2))
        )

    "robots are not routed" should:

      "do nothing" in:
        Phase.moving(world) shouldBe empty

    "robots are blocked" should:

      "unblock if they can move" in:
        Phase.assigning(world)
        world.route(r1, Path(Step(p3, Tick.unit), Step(p4, Tick.unit)))
        world.route(r2, Path(Step(p3, Tick.unit), Step(p4, Tick.unit)))
        Phase.collisionHandling(world)
        Phase.expiring(world)
        Phase.moving(world) should matchPattern {
          case Seq(Event.RobotMoved(_, _, _)) =>
        }
        Phase.collisionHandling(world) should matchPattern {
          case Seq(Event.RobotUnblocked(_)) =>
        }
        Phase.expiring(world)
        Phase.moving(world) should matchPattern {
          case Seq(Event.RobotMoved(_, _, _), Event.RobotMoved(_, _, _)) =>
        }

  "The performing phase" when:

    "a robot stands on the destination of its mission" should:

      "complete the mission" in:
        Phase.assigning(world)
        Phase.routing(world)
        Phase.expiring(world)
        Phase.moving(world)

        Phase.performing(world) should contain theSameElementsAs Seq(
          Event.MissionCompleted(m1)
        )
        world.mission(m1).value.status shouldBe MissionStatus.Completed
        world.robot(r1).value.status shouldBe RobotStatus.Idle

    "a robot has not reached its destination" should:

      "leave it untouched" in:
        Phase.assigning(world)
        Phase.routing(world)
        Phase.expiring(world)
        Phase.moving(world)

        Phase.performing(world) should not contain Event.MissionCompleted(m2)
        world.mission(m2).value.status shouldBe MissionStatus.Assigned
        world.robot(r2).value.status shouldBe RobotStatus.Moving

    "The expiring phase" when:

      "the duration of a mission is exhausted" should:

        "signal its failure" in:
          val events =
            for
              _ <- 1 to deadline
              event <- Phase.expiring(world)
            yield event

          events should contain theSameElementsAs Seq(
            Event.MissionFailed(m1),
            Event.MissionFailed(m2)
          )
