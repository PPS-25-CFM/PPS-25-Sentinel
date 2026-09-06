package it.unibo.sentinel.core.item

import it.unibo.sentinel.UnitTest

class ItemSpec extends UnitTest:

  "An Item" when:

    "queried for its weight" should:

      "expose the expected weights" in:
        Item.Computer.weight.value.shouldBe(1.0)
        Item.Table.weight.value.shouldBe(10.0)
        Item.Fridge.weight.value.shouldBe(50.0)
        Item.Dishwasher.weight.value.shouldBe(50.0)

    "aggregated" should:

      "compute highestWeight as 50.0" in:
        Item.highestWeight.value.shouldBe(50.0)

      "compute averageWeight as round((1+10+50+50)/4) = 28" in:
        Item.averageWeight.value.shouldBe(28.0)

  "An ItemWeight" when:

    "created with a negative value" should:

      "be capped at Zero" in:
        ItemWeight(-5.0).value.shouldBe(ItemWeight.Zero.value)
        ItemWeight(-5.0).value.shouldBe(0.0)

    "created with a positive value" should:

      "preserve the value" in:
        ItemWeight(12.5).value.shouldBe(12.5)

    "deconstructed with unapply" should:

      "return the raw Double" in:
        ItemWeight.unapply(Item.Computer.weight).shouldBe(Some(1.0))

    "combined" should:

      "add correctly" in:
        (ItemWeight(1.0) + ItemWeight(10.0)).value.shouldBe(11.0)

      "subtract flooring at Zero" in:
        (ItemWeight(1.0) - ItemWeight(10.0)).value.shouldBe(0.0)
        (ItemWeight(10.0) - ItemWeight(1.0)).value.shouldBe(9.0)

    "ordered" should:

      "support sorting" in:
        Seq(
          Item.Table.weight,
          Item.Computer.weight,
          Item.Fridge.weight
        ).sorted.headOption.value.shouldBe(
          Item.Computer.weight
        )
