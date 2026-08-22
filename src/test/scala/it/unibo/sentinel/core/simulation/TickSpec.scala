package it.unibo.sentinel.core.simulation

import it.unibo.sentinel.UnitTest

class TickSpec extends UnitTest:
  "A Tick" when:

    "created" should:

      "throw an IllegalArgumentException if created with negative value" in:
        an[IllegalArgumentException] should be thrownBy Tick(-1)

      "expose the given value" in:
        val tick = Tick(5)
        tick.value shouldBe 5

    "asked for the previous tick" should:

      "return 0 if the current tick is 0" in:
        val tick = Tick(0)
        tick.previous shouldBe Tick(0)

      "return the previous tick otherwise" in:
        val tick = Tick(5)
        tick.previous shouldBe Tick(4)

    "asked for the next tick" should:

      "return the next tick" in:
        val tick = Tick(5)
        tick.next shouldBe Tick(6)
