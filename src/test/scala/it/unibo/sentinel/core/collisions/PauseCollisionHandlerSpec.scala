package it.unibo.sentinel.core.collisions

import it.unibo.sentinel.UnitTest
import it.unibo.sentinel.core.warehouse.Position

class PauseCollisionHandlerSpec extends UnitTest with CollisionHandlerBehavior:

  private val pausing: CollisionHandler = CollisionHandler.pause()

  "A CollisionHandler.pause" when:

    correctCollisionResolver(pausing)

    "resolving indirect collisions" should:

      "make the yielding loser wait" in:
        val target = Position(1, 1)
        val i1 = moveIntent(r1, Position(0, 0), target)
        val i2 = moveIntent(r2, Position(0, 1), target)
        pausing.resolveCollisions(Seq(i1, i2)) shouldBe Map(
          r1 -> Action.Move,
          r2 -> Action.Wait
        )

      "make all contenders wait if a stationary robot occupies the target cell" in:
        val target = Position(1, 1)
        val i1 = moveIntent(r1, Position(0, 0), target)
        val i2 = moveIntent(r2, Position(0, 1), target)
        val i3Stat = stationaryIntent(r3, target)
        val actions = pausing.resolveCollisions(Seq(i1, i2, i3Stat))
        actions(r1) shouldBe Action.Wait
        actions(r2) shouldBe Action.Wait

    "resolving direct collisions" should:

      "make both robots wait" in:
        val i1 = moveIntent(r1, Position(0, 0), Position(1, 0))
        val i2 = moveIntent(r2, Position(1, 0), Position(0, 0))
        pausing.resolveCollisions(Seq(i1, i2)) shouldBe Map(
          r1 -> Action.Wait,
          r2 -> Action.Wait
        )

    "handling chain dependencies" should:

      "cascade wait decisions when a robot cannot move into an occupied cell" in:
        val i1 = moveIntent(r1, Position(0, 0), Position(1, 0))
        val i2 = moveIntent(r2, Position(1, 0), Position(2, 0))
        val i3 = moveIntent(r3, Position(2, 0), Position(3, 0))
        val i4Stat = stationaryIntent(r4, Position(3, 0))
        val actions = pausing.resolveCollisions(Seq(i1, i2, i3, i4Stat))
        actions shouldBe Map(
          r1 -> Action.Wait,
          r2 -> Action.Wait,
          r3 -> Action.Wait
        )
