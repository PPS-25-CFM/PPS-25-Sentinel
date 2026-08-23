package it.unibo.sentinel.core.simulation

import it.unibo.sentinel.UnitTest
import it.unibo.sentinel.core.assignment.Selector
import it.unibo.sentinel.core.routing.Navigator
import it.unibo.sentinel.core.warehouse.Warehouse
import org.scalatest.BeforeAndAfterEach

import scala.compiletime.uninitialized

class PhaseSpec
    extends UnitTest
    with BeforeAndAfterEach
    with EnvironmentFixture:

  given Warehouse = warehouse
  given navigator: Navigator = scenario.routing()
  given selector: Selector = scenario.assignment()

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
