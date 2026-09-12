package it.unibo.sentinel.control

import monix.reactive.Observable

/** An [[Editor]] is a component that interprets user commands as
  * transformations of a state.
  */
trait Editor:
  /** The state of the [[Editor]].
    */
  type State

  /** The command that the user can trigger to change the [[State]].
    */
  type Command

  /** The model the [[Editor]] edits.
    */
  type Model

  /** Extracts the [[Model]] from the current [[State]].
    *
    * @param state
    *   the current [[State]] of the [[Editor]].
    * @return
    *   the extracted [[Model]] from `state`.
    */
  def model(state: State): Model

  /** Applies a command to the current state.
    * @param state
    *   the current [[State]].
    * @param command
    *   the [[Command]] to apply.
    * @return
    *   the new [[State]] after applying the command.
    */
  def apply(state: State, command: Command): State

  /** @param initial
    * @param commands
    * @return
    */
  def execute(
      initial: State,
      commands: Observable[Command]
  ): Observable[State] =
    commands.scan(initial)(apply)
