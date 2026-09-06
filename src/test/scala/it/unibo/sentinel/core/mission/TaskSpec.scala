package it.unibo.sentinel.core.mission

import it.unibo.sentinel.UnitTest
import it.unibo.sentinel.core.item.Item
import it.unibo.sentinel.core.warehouse.Position

class TaskSpec extends UnitTest:
  val at: Position = Position(2, 2)
  val to: Position = Position(3, 3)
  val move: Action = Action.Move(to)
  val pick: Action = Action.PickUp(Item.Computer, at)
  val drop: Action = Action.Drop(Item.Computer, to)

  "A Task" when:

    "it is Done" should:

      "have no current action and no actions" in:
        Task.Done.currentAction shouldBe None
        Task.Done.actions.toSeq shouldBe empty

      "stay Done on advance" in:
        Task.Done.advance shouldBe Task.Done

    "it is Single" should:

      "expose its action" in:
        Task.Single(move).currentAction shouldBe Some(move)
        Task.Single(move).actions.toSeq should contain only move

      "become Done on advance" in:
        Task.Single(move).advance shouldBe Task.Done

    "it is Then" should:

      "delegate currentAction to the head" in:
        Task
          .Then(Task.Single(pick), Task.Single(drop))
          .currentAction shouldBe Some(pick)

      "concatenate actions in order" in:
        val task = Task.Then(Task.Single(pick), Task.Single(drop))
        task.actions.toSeq should contain inOrderOnly (pick, drop)

      "expose nested sequences in order" in:
        val task = Task.Then(
          Task.Then(Task.Single(pick), Task.Single(drop)),
          Task.Single(move)
        )
        task.actions.toSeq should contain inOrderOnly (pick, drop, move)

      "advance to the tail" in:
        val tail = Task.Single(drop)
        Task.Then(Task.Single(pick), tail).advance shouldBe tail

    "created with factories" should:

      "build a move task" in:
        Task.move(to) shouldBe Task.Single(Action.Move(to))

      "build pick and drop tasks" in:
        Task.pick(Item.Computer, at) shouldBe Task.Single(
          Action.PickUp(Item.Computer, at)
        )
        Task.drop(Item.Computer, to) shouldBe Task.Single(
          Action.Drop(Item.Computer, to)
        )

      "build a pickAndDrop task emitting pick then drop" in:
        val task = Task.pickAndDrop(Item.Computer, at, to)
        task.actions.toSeq should contain inOrderOnly (pick, drop)
        task.currentAction shouldBe Some(pick)
        task.advance.currentAction shouldBe Some(drop)
        task.advance.advance shouldBe Task.Done
