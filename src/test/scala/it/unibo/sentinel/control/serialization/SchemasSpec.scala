package it.unibo.sentinel.control.serialization

import it.unibo.sentinel.UnitTest
import it.unibo.sentinel.control.serialization.Codec.Validation
import it.unibo.sentinel.control.serialization.schemas.*
import it.unibo.sentinel.core.item.Item
import it.unibo.sentinel.core.mission.Mission
import it.unibo.sentinel.core.mission.MissionId
import it.unibo.sentinel.core.scenario.RobotClass
import it.unibo.sentinel.core.warehouse.Tile

class SchemasSpec extends UnitTest:

  "An ItemSchema" when:

    "validated" should:

      "accept positive weights" in:
        ItemSchema
          .Computer(1.0)
          .validated
          .shouldBe(Right(ItemSchema.Computer(1.0)))
        ItemSchema.Fridge(50.0).validated.isRight.shouldBe(true)

      "reject zero or negative weights as ItemValidation" in:
        ItemSchema
          .Table(0.0)
          .validated
          .shouldBe(
            Left(Validation.ItemValidation(Item.Validation.NegativeWeight(0.0)))
          )
        ItemSchema
          .Dishwasher(-3.0)
          .validated
          .shouldBe(
            Left(
              Validation.ItemValidation(Item.Validation.NegativeWeight(-3.0))
            )
          )

  "A TileSchema" when:

    "validated" should:

      "delegate Shelf validation to the inner ItemSchema" in:
        TileSchema
          .Shelf(ItemSchema.Computer(1.0))
          .validated
          .shouldBe(
            Right(TileSchema.Shelf(ItemSchema.Computer(1.0)))
          )
        TileSchema
          .Shelf(ItemSchema.Table(-1.0))
          .validated
          .shouldBe(
            Left(
              Validation.ItemValidation(Item.Validation.NegativeWeight(-1.0))
            )
          )

      "reject negative LoadingBay costs" in:
        TileSchema
          .LoadingBay(-5)
          .validated
          .shouldBe(
            Left(Validation.TileValidation(Tile.Validation.NegativeCost(-5)))
          )
        TileSchema
          .LoadingBay(2)
          .validated
          .shouldBe(Right(TileSchema.LoadingBay(2)))

      "reject negative Floor costs" in:
        TileSchema
          .Floor(-1)
          .validated
          .shouldBe(
            Left(Validation.TileValidation(Tile.Validation.NegativeCost(-1)))
          )

  "An ActionSchema" when:

    "validated" should:

      "accept a valid Move" in:
        ActionSchema
          .Move(PositionSchema(1, 1))
          .validated
          .shouldBe(
            Right(ActionSchema.Move(PositionSchema(1, 1)))
          )

      "propagate Item errors from PickUp and Drop" in:
        val badPick =
          ActionSchema.PickUp(ItemSchema.Computer(-1.0), PositionSchema(0, 0))
        badPick.validated.shouldBe(
          Left(Validation.ItemValidation(Item.Validation.NegativeWeight(-1.0)))
        )
        val badDrop =
          ActionSchema.Drop(ItemSchema.Table(0.0), PositionSchema(0, 0))
        badDrop.validated.shouldBe(
          Left(Validation.ItemValidation(Item.Validation.NegativeWeight(0.0)))
        )

      "accept valid PickUp and Drop" in:
        ActionSchema
          .PickUp(ItemSchema.Computer(1.0), PositionSchema(0, 0))
          .validated
          .isRight
          .shouldBe(true)
        ActionSchema
          .Drop(ItemSchema.Table(10.0), PositionSchema(2, 2))
          .validated
          .isRight
          .shouldBe(true)

  "A TaskSchema" when:

    "validated" should:

      "validate both branches of Then" in:
        val good = TaskSchema.Then(
          TaskSchema.Single(ActionSchema.Move(PositionSchema(0, 0))),
          TaskSchema.Single(
            ActionSchema.PickUp(ItemSchema.Computer(1.0), PositionSchema(1, 1))
          )
        )
        good.validated.shouldBe(Right(good))
        val bad = TaskSchema.Then(
          TaskSchema.Single(ActionSchema.Move(PositionSchema(0, 0))),
          TaskSchema.Single(
            ActionSchema.Drop(ItemSchema.Fridge(-2.0), PositionSchema(1, 1))
          )
        )
        bad.validated.shouldBe(
          Left(Validation.ItemValidation(Item.Validation.NegativeWeight(-2.0)))
        )

  "A MissionSchema" when:

    "validated" should:

      "reject non-positive durations" in:
        MissionSchema(
          "M1",
          TaskSchema.Single(ActionSchema.Move(PositionSchema(0, 0))),
          0
        ).validated.shouldBe(
          Left(
            Validation.MissionValidation(
              Mission.Validation.NegativeDuration(MissionId("M1"), 0)
            )
          )
        )

  "A SpawnSchema" when:

    "validated" should:

      "preserve ofClass and delegate position validation" in:
        SpawnSchema("R1", PositionSchema(1, 1), RobotClass.Drone).validated
          .shouldBe(
            Right(SpawnSchema("R1", PositionSchema(1, 1), RobotClass.Drone))
          )
