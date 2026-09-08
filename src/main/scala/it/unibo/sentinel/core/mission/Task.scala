package it.unibo.sentinel.core.mission

import it.unibo.sentinel.core.warehouse.Position
import it.unibo.sentinel.core.item.Item

/** An ordered composite of [[Action]]s that defines the work of a [[Mission]].
  */
enum Task:
  /** Sequential composition: execute `head` then `tail`.
    *
    * @param head
    *   the first task to execute.
    * @param tail
    *   the task to execute after `head`.
    */
  case Then(head: Task, tail: Task)

  /** A single atomic [[Action]].
    *
    * @param action
    *   the action to perform.
    */
  case Single(action: Action)

  /** Terminal task with no remaining actions. */
  case Done

  /** @return the first pending [[Action]], if any. */
  def currentAction: Option[Action] = this match
    case Then(head, _)  => head.currentAction
    case Single(action) => Some(action)
    case Done           => None

  /** @return the task that remains after completing the current action. */
  def advance: Task = this match
    case Then(_, tail) => tail
    case Single(_)     => Done
    case Done          => Done

  /** @return all [[Action]]s in execution order. */
  def actions: Iterator[Action] = this match
    case Done       => Iterator.empty
    case Single(a)  => Iterator.single(a)
    case Then(h, t) => h.actions ++ t.actions

  /** @return
    *   true if the task requires only movement.
    */
  def isMovementOnly: Boolean = actions.forall:
    case Action.Move(_) => true
    case _              => false

  /** @return
    *   true if the task requires picking up or dropping items.
    */
  def requiresCarrying: Boolean = actions.exists:
    case Action.PickUp(_, _) | Action.Drop(_, _) => true
    case _                                       => false

object Task:
  /** @param to
    *   destination to move towards.
    * @return
    *   a single-move [[Task]].
    */
  def move(to: Position): Task = Single(Action.Move(to))

  /** @param item
    *   item to pick.
    * @param at
    *   shelf position.
    * @return
    *   a single pick-up [[Task]].
    */
  def pick(item: Item, at: Position): Task = Single(Action.PickUp(item, at))

  /** @param item
    *   item to drop.
    * @param at
    *   loading bay position.
    * @return
    *   a single drop [[Task]].
    */
  def drop(item: Item, at: Position): Task = Single(Action.Drop(item, at))

  /** @param item
    *   item to transport.
    * @param at
    *   shelf position to pick from.
    * @param to
    *   loading bay position to drop onto.
    * @return
    *   a pick-then-drop [[Task]].
    */
  def pickAndDrop(item: Item, at: Position, to: Position): Task =
    Then(pick(item, at), drop(item, to))
