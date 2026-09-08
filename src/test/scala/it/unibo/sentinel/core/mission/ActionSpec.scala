package it.unibo.sentinel.core.mission

import it.unibo.sentinel.UnitTest
import it.unibo.sentinel.core.item.Item
import it.unibo.sentinel.core.warehouse.Position

class ActionSpec extends UnitTest:
  val at: Position = Position(2, 2)
  val to: Position = Position(3, 3)

  "An Action" when:

    "it is a Move" should:

      "expose its destination as position" in:
        Action.Move(to).position shouldBe to

    "it is a PickUp" should:

      "expose its shelf position as position" in:
        Action.PickUp(Item.Computer, at).position shouldBe at

    "it is a Drop" should:

      "expose its bay position as position" in:
        Action.Drop(Item.Computer, to).position shouldBe to
