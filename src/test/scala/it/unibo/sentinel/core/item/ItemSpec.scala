package it.unibo.sentinel.core.item

import it.unibo.sentinel.UnitTest

class ItemSpec extends UnitTest:

  "An Item" when:

    "queried for its weight" should:

      "expose the expected weights" in:
        Item.Computer.weight shouldBe Weight(1.0)
        Item.Table.weight shouldBe Weight(10.0)
        Item.Fridge.weight shouldBe Weight(50.0)
        Item.Dishwasher.weight shouldBe Weight(50.0)

  "An ItemWeight" when:

    "created with a negative value" should:

      "be capped at zero" in:
        Weight(-5.0) shouldBe Weight.zero

    "created with a positive value" should:

      "preserve the value" in:
        Weight(12.5) shouldBe Weight(12.5)

    "deconstructed with unapply" should:

      "return the raw Double" in:
        Weight.unapply(Item.Computer.weight) shouldBe Some(1.0)

    "combined" should:

      "add correctly" in:
        (Weight(1.0) + Weight(10.0)) shouldBe Weight(11.0)

      "subtract flooring at Zero" in:
        (Weight(1.0) - Weight(10.0)) shouldBe Weight.zero
        (Weight(10.0) - Weight(1.0)) shouldBe Weight(9.0)

    "ordered" should:

      "support sorting" in:
        Seq(
          Item.Table.weight,
          Item.Computer.weight,
          Item.Fridge.weight
        ).sorted.headOption.value shouldBe Item.Computer.weight
