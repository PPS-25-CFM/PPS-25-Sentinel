package it.unibo.sentinel.core.warehouse

import it.unibo.sentinel.UnitTest
import it.unibo.sentinel.core.item.Item
import it.unibo.sentinel.core.simulation.Tick

class TileSpec extends UnitTest:
  "A Floor" when:

    "created with no parameters" should:

      "take 1 tick to be crossed" in:
        val floor = Tile.Floor()
        floor.cost shouldBe Tick(1)

    "created with a specific tick" should:

      "take the given number of ticks to be crossed" in:
        val floor = Tile.Floor(Tick(5))
        floor.cost shouldBe Tick(5)

    "checked for traits" should:

      "be walkable but not interactable" in:
        val floor = Tile.Floor()
        floor shouldBe a[Tile.Walkable]
        floor should not be a[Tile.Interactable]

  "A Shelf" when:

    "created" should:

      "hold the given item" in:
        Tile.Shelf(Item.Computer).item shouldBe Item.Computer

      "be interactable but not walkable" in:
        val shelf = Tile.Shelf(Item.Table)
        shelf shouldBe a[Tile.Interactable]
        shelf should not be a[Tile.Walkable]

      "be interacted with from the four orthogonal neighbours" in:
        val shelf = Tile.Shelf(Item.Fridge)
        shelf.interactiveOffset should contain theSameElementsAs Seq(
          Position(1, 0),
          Position(0, 1),
          Position(-1, 0),
          Position(0, -1)
        )

  "A LoadingBay" when:

    "created with no parameters" should:

      "take 1 tick to be crossed" in:
        Tile.LoadingBay().cost shouldBe Tick(1)

      "be both walkable and interactable" in:
        val bay = Tile.LoadingBay()
        bay shouldBe a[Tile.Walkable]
        bay shouldBe a[Tile.Interactable]

      "be interacted with while standing on it" in:
        Tile.LoadingBay().interactiveOffset should contain only Position(0, 0)

    "created with a specific tick" should:

      "take the given number of ticks to be crossed" in:
        Tile.LoadingBay(Tick(3)).cost shouldBe Tick(3)
