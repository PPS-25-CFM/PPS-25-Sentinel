package it.unibo.sentinel.control

import it.unibo.sentinel.core.warehouse.{Area, Position, Tile, Warehouse}

/** Interprets user editing commands as transformations of a [[Warehouse]] under
  * construction.
  */
object WarehouseEditor:

  /** What the user can do while editing a [[Warehouse]].
    */
  enum Command:
    /** Selects a single cell, replacing any previous selection.
      * @param at
      *   the selected position.
      */
    case Select(at: Position)

    /** Extends the current selection into a rectangular area having `to` as its
      * opposite corner.
      * @param to
      *   the new corner of the selection.
      */
    case ExtendSelection(to: Position)

    /** Applies `tile` to every position of the current selection.
      * @param tile
      *   the tile to apply.
      */
    case Apply(tile: Tile)

    /** Removes the tile at every position of the current selection.
      */
    case Remove

  /** The state of an in-progress edit.
    *
    * @param warehouse
    *   the [[Warehouse]] as edited so far.
    * @param selection
    *   the currently selected [[Area]], if any.
    */
  final case class State(warehouse: Warehouse, selection: Option[Area] = None)

  /** @param state
    *   the current editing state.
    * @param command
    *   the command issued by the user.
    * @return
    *   the [[State]] resulting from applying `command` to `state`.
    */
  def reduce(state: State, command: Command): State = command match
    case Command.Select(at) =>
      state.copy(selection = Some(Area(at, at)))
    case Command.ExtendSelection(to) =>
      val corner = state.selection.map(_.corner).getOrElse(to)
      state.copy(selection = Some(Area(corner, to)))
    case Command.Apply(tile) =>
      state.copy(
        warehouse = state.selection.fold(state.warehouse):
          state.warehouse.withArea(_)(tile)
      )
    case Command.Remove =>
      state.copy(
        warehouse = state.selection.fold(state.warehouse): area =>
          area.positions.foldLeft(state.warehouse)(_.withoutTile(_))
      )
