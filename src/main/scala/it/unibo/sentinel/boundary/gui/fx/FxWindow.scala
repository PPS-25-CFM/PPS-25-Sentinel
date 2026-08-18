package it.unibo.sentinel.boundary.gui.fx

import it.unibo.sentinel.boundary.gui.toolkit.Window
import scalafx.stage.Stage

/** [[Window]] implementation based on the fx library
  */
final class FxWindow extends Window:

  private lazy val stage: Stage = new Stage()

  override type V[Model] = FxView[Model]

  override def open(): Unit = stage.show()

  override def close(): Unit = stage.close()

  override def show[Model](view: V[Model]): Unit = stage.scene = view.scene
