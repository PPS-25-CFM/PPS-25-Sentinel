package it.unibo.sentinel.boundary.gui.toolkit

import it.unibo.sentinel.control.WarehouseEditor

/** A [[View]] for [[Editor]]s.
  */
trait EditorView[State, Command]
    extends View[State],
      Interactive[Command],
      Dismissable

type WarehouseEditorView =
  EditorView[WarehouseEditor.State, WarehouseEditor.Command]
