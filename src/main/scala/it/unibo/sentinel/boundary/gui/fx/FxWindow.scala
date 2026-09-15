package it.unibo.sentinel.boundary.gui.fx

import it.unibo.sentinel.boundary.gui.toolkit.Window
import it.unibo.sentinel.boundary.gui.fx.FxUtils.onFx
import monix.eval.Task
import monix.execution.CancelablePromise
import scalafx.Includes.{eventClosureWrapperWithParam, jfxWindowEvent2sfx}
import scalafx.stage.{Screen, Stage, WindowEvent}

/** [[Window]] implementation based on the fx library
  */
final class FxWindow(
    defaultWidth: Option[Double] = None,
    defaultHeight: Option[Double] = None
) extends Window[FxView]:

  private val closeRequest = CancelablePromise[Unit]()

  private lazy val stage: Stage = new Stage():
    private val screen = Screen.primary.visualBounds
    defaultWidth.foreach(w => width = math.min(w, screen.width * 0.9))
    defaultHeight.foreach(h => height = math.min(h, screen.height * 0.9))
    onCloseRequest = (_: WindowEvent) =>
      val _ = closeRequest.trySuccess(())

  override def open(): Task[Unit] =
    onFx(stage.show()) >> Task.fromCancelablePromise(closeRequest)

  override def show(view: FxView): Task[Unit] = onFx:
    stage.scene = view.scene
