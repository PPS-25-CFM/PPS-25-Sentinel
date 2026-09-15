package it.unibo.sentinel.boundary.gui.toolkit

import it.unibo.sentinel.control.{ScenarioEditor, WarehouseEditor}

/** A [[View]] for [[Editor]]s.
  */
trait EditorView[State, Command]
    extends InteractiveView[State, Command]
    with Dismissable

type WarehouseEditorView =
  EditorView[WarehouseEditor.State, WarehouseEditor.Command]

type ScenarioEditorView =
  EditorView[ScenarioEditor.State, ScenarioEditor.Command]
