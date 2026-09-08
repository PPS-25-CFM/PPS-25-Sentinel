package it.unibo.sentinel.core.simulation

import it.unibo.sentinel.UnitTest
import it.unibo.sentinel.core.assignment.Selector
import it.unibo.sentinel.core.routing.Navigator
import it.unibo.sentinel.core.warehouse.Warehouse
import org.scalatest.BeforeAndAfterEach

import scala.compiletime.uninitialized
import it.unibo.sentinel.core.warehouse.{Position, Tile}
import it.unibo.sentinel.core.item.Item
import it.unibo.sentinel.core.mission.{Mission, MissionId, MissionStatus}
import it.unibo.sentinel.core.robot.RobotStatus
import it.unibo.sentinel.core.scenario.{RobotClass, Scenario, Spawn}
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
  given SelectionPolicy = scenario.collisionSelection()(using scenario.missions)
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

    "a carrier has a pick mission" should:

      "route to an interaction point of the shelf, not onto the shelf" in:
        val (depWorld, wh, depNav, depSel) = depositSetup(spawnAt = p1)
        Phase.assigning(using depSel)(depWorld)

        val routed = Phase.routing(using depNav)(depWorld)
        routed should not be empty

        val paths = routed.collect { case Event.RobotRouted(_, path) => path }
        paths should not be empty
        forAll(paths): path =>
          path should not be empty
          path.lastOption.value should not be shelf
          wh.interactionPoints(shelf) should contain(path.lastOption.value)

  "The handle collisions phase" when:

    "there are colliding robots" should:

      "pause one robot and let the other proceed" in:
        Phase.assigning(world)
        world.route(r1, Path(Step(p3, Tick.unit), Step(p4, Tick.unit)))
        world.route(r2, Path(Step(p3, Tick.unit), Step(p4, Tick.unit)))
        Phase.expiring(world)
        Phase.expiring(world)
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
        world.route(
          r1,
          Path(Step(Position(2, 1), Tick.unit), Step(Position(2, 2), Tick.unit))
        )
        world.route(
          r2,
          Path(Step(Position(2, 1), Tick.unit), Step(Position(2, 2), Tick.unit))
        )
        Phase.expiring(world)
        Phase.collisionHandling(world) should matchPattern {
          case Seq(Event.RobotBlocked(_, _)) =>
        }
        Phase.moving(world) should matchPattern {
          case Seq(Event.RobotMoved(_, _, _)) =>
        }
        Phase.expiring(world)
        Phase.collisionHandling(world) should matchPattern {
          case Seq(Event.RobotUnblocked(_)) =>
        }
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

    "a carrier stands on the shelf interaction point" should:

      "emit ItemPicked and stay Assigned" in:
        val spot = depositWarehouse.interactionPoints(shelf).headOption.value
        val (depWorld, _, _, depSel) = depositSetup(spawnAt = spot)
        Phase.assigning(using depSel)(depWorld)

        Phase.performing(depWorld) should contain(
          Event.ItemPicked(r1, depId, Item.Computer, shelf)
        )
        depWorld.mission(depId).value.status shouldBe MissionStatus.Assigned
        depWorld.robot(r1).value.status shouldBe RobotStatus.Ready

    "a carrier stands on the bay after picking" should:

      "emit ItemDropped and MissionCompleted together" in:
        val (depWorld, _, _, depSel) = depositSetup(spawnAt = bay)
        Phase.assigning(using depSel)(depWorld)
        // advance PickUp ignoring position, so current action becomes Drop at bay
        depWorld.perform(r1) shouldBe Seq(
          Event.ItemPicked(r1, depId, Item.Computer, shelf)
        )

        Phase.performing(depWorld) should contain theSameElementsAs Seq(
          Event.ItemDropped(r1, depId, Item.Computer, bay),
          Event.MissionCompleted(depId)
        )
        depWorld.mission(depId).value.status shouldBe MissionStatus.Completed
        depWorld.robot(r1).value.mission shouldBe None

  "The expiring phase" when:

    "the duration of a mission is exhausted" should:

      "signal its failure" in:
        val events =
          for
            _ <- 1 to deadline.value
            event <- Phase.expiring(world)
          yield event

        events should contain theSameElementsAs Seq(
          Event.MissionFailed(m1),
          Event.MissionFailed(m2)
        )

  private val depId: MissionId = MissionId("D1")
  private val shelf: Position = Position(2, 2)
  private val bay: Position = Position(3, 3)

  private def depositWarehouse: Warehouse =
    warehouse
      .withTile(shelf)(Tile.Shelf(Item.Computer))
      .withTile(bay)(Tile.LoadingBay())

  private def depositSetup(
      spawnAt: Position
  ): (Environment, Warehouse, Navigator, Selector) =
    val wh = depositWarehouse
    val depScenario = (for
      s0 <- Right(Scenario.in(wh))
      s1 <- s0.place(Spawn(r1, spawnAt, RobotClass.Carrier))
      s2 <- s1.load(Mission.deliver(depId, Item.Computer, shelf, bay, deadline))
    yield s2).value
    val depNav = depScenario.routing()(using wh)
    val depSel = depScenario.assignment()(using depNav)
    (depScenario.build, wh, depNav, depSel)
