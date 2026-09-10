package it.unibo.sentinel.control

import it.unibo.sentinel.UnitTest
import it.unibo.sentinel.control.WarehouseEditor.{Command, State}
import it.unibo.sentinel.core.item.Item
import it.unibo.sentinel.core.warehouse.{
  Area,
  Position,
  Tile,
  Warehouse,
  WarehouseId
}

trait WarehouseEditorFixture:
  self: UnitTest =>
  val warehouse: Warehouse = Warehouse.empty(WarehouseId("W1"), 5, 5)
  val initial: State = State(warehouse)
  val corner: Position = Position(1, 1)
  val opposite: Position = Position(3, 2)

class WarehouseEditorSpec extends UnitTest with WarehouseEditorFixture:
  "The WarehouseEditor" when:

    "a cell is selected" should:

      "replace any previous selection with a single-cell area" in:
        val selected = WarehouseEditor.reduce(initial, Command.Select(corner))
        selected.selection.value shouldBe Area(corner, corner)

      "override a previously extended selection" in:
        val extended = WarehouseEditor.reduce(
          WarehouseEditor.reduce(initial, Command.ExtendSelection(opposite)),
          Command.Select(corner)
        )
        extended.selection.value shouldBe Area(corner, corner)

    "the selection is extended" should:

      "create a single-cell area if there was no prior selection" in:
        val extended =
          WarehouseEditor.reduce(initial, Command.ExtendSelection(opposite))
        extended.selection.value shouldBe Area(opposite, opposite)

      "create a rectangle between the previous corner and the new one" in:
        val selected = WarehouseEditor.reduce(initial, Command.Select(corner))
        val extended =
          WarehouseEditor.reduce(selected, Command.ExtendSelection(opposite))
        extended.selection.value shouldBe Area(corner, opposite)

    "a tile is applied" should:

      "paint every position of the current selection" in:
        val selected = WarehouseEditor.reduce(initial, Command.Select(corner))
        val extended =
          WarehouseEditor.reduce(selected, Command.ExtendSelection(opposite))
        val painted =
          WarehouseEditor.reduce(extended, Command.Apply(Tile.Floor()))
        forAll(Area(corner, opposite).positions):
          painted.warehouse.tileAt(_).value shouldBe Tile.Floor()

      "leave the warehouse unchanged if there is no selection" in:
        val painted = WarehouseEditor.reduce(
          initial,
          Command.Apply(Tile.Shelf(Item.Computer))
        )
        painted.warehouse shouldBe initial.warehouse

    "a tile is removed" should:

      "clear every position of the current selection" in:
        val selected = WarehouseEditor.reduce(initial, Command.Select(corner))
        val extended =
          WarehouseEditor.reduce(selected, Command.ExtendSelection(opposite))
        val painted =
          WarehouseEditor.reduce(extended, Command.Apply(Tile.Floor()))
        val removed = WarehouseEditor.reduce(painted, Command.Remove)
        forAll(Area(corner, opposite).positions):
          removed.warehouse.tileAt(_) shouldBe None

      "leave the warehouse unchanged if there is no selection" in:
        val removed = WarehouseEditor.reduce(initial, Command.Remove)
        removed.warehouse shouldBe initial.warehouse

    "a full paint-then-erase sequence is applied" should:

      "reproduce the same edits the GUI would drive" in:
        val result = Seq(
          Command.Select(corner),
          Command.ExtendSelection(opposite),
          Command.Apply(Tile.Shelf(Item.Computer)),
          Command.Remove
        ).foldLeft(initial)(WarehouseEditor.reduce)

        forAll(Area(corner, opposite).positions):
          result.warehouse.tileAt(_) shouldBe None
