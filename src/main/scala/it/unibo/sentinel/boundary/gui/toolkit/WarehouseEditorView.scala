package it.unibo.sentinel.boundary.gui.toolkit

import it.unibo.sentinel.control.WarehouseEditor

/** A [[View]] that visualizes an in-progress [[WarehouseEditor.State]] and is
  * commanded through [[WarehouseEditor.Command]]s.
  */
trait WarehouseEditorView
    extends View[WarehouseEditor.State],
      Interactive[WarehouseEditor.Command],
      Dismissable
