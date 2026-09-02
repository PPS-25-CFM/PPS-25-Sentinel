package it.unibo.sentinel.boundary.gui.fx

import it.unibo.sentinel.boundary.gui.toolkit.Window
import it.unibo.sentinel.boundary.gui.fx.FxUtils.onFx
import scalafx.stage.Stage

/** [[Window]] implementation based on the fx library
  */
final class FxWindow(
    defaultWidth: Option[Double] = None,
    defaultHeight: Option[Double] = None
) extends Window:

  private lazy val stage: Stage = new Stage():
    defaultWidth.foreach(w => width = w)
    defaultHeight.foreach(h => height = h)

  override type V = FxView

  override def open(): Unit = onFx(stage.show())

  override def close(): Unit = onFx(stage.close())

  override def show(view: V): Unit = onFx:
    stage.scene = view.scene
