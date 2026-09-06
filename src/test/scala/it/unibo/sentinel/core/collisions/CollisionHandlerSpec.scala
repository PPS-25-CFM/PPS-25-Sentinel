package it.unibo.sentinel.core.collisions

import it.unibo.sentinel.UnitTest
import it.unibo.sentinel.core.robot.RobotStatus
import it.unibo.sentinel.core.scenario.Placement
import it.unibo.sentinel.core.warehouse.Position

trait CollisionHandlerFixture extends CollisionCheckerFixture:
  self: UnitTest =>

  given SelectionPolicy = SelectionPolicy.random()
  val pausing: CollisionHandler = CollisionHandler.pausing()
  val placements: Seq[Placement] = Seq(
    Placement(r1, Position(0, 1)),
    Placement(r2, Position(1, 0)),
    Placement(r3, Position(2, 1))
  )
  placements.foreach(_.robot.tick())

class CollisionHandlerSpec extends UnitTest with CollisionHandlerFixture:

  "A wait-based collision handler using random choice" when:

    "handling collisions" should:

      "pause all but one random robot" in:
        pausing.resolveCollisions(placements)
        forExactly(1, group1) { robot =>
          robot.status should not be RobotStatus.Waiting
        }
