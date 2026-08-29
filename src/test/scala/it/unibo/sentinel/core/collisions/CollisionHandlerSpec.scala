package it.unibo.sentinel.core.collisions

import it.unibo.sentinel.UnitTest
import it.unibo.sentinel.core.robot.RobotStatus
import it.unibo.sentinel.core.scenario.Placement
import it.unibo.sentinel.core.warehouse.Position

trait CollisionHandlerFixture extends CollisionCheckerFixture:
  self: UnitTest =>

  given SelectionPolicy = SelectionPolicy.random()
  val handler: CollisionHandler = CollisionHandler.pausing()

class CollisionHandlerSpec extends UnitTest with CollisionHandlerFixture:

  "A wait-based collision handler using random choice" when:

    "handling collisions" should:

      "pause all but one random robot" in:
        val placements = group1.map(r => Placement(r, Position(0, 0)))
        handler.resolveCollisions(placements)
        forExactly(1, group1) { robot =>
          robot.status should not be RobotStatus.Waiting
        }
