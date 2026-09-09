package it.unibo.sentinel.boundary.gui.fx

import it.unibo.sentinel.boundary.gui.toolkit.Window
import it.unibo.sentinel.boundary.gui.fx.FxUtils.onFx
import scalafx.scene.control.Alert
import scalafx.stage.{FileChooser, Stage}

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

  override def onClose(action: () => Unit): Unit = onFx:
    stage.onHidden() = _ => action()

  override def show(view: V): Unit = onFx:
    stage.scene = view.scene

  override def chooseJsonFile(): Option[os.Path] =
    val chooser = new FileChooser:
      title = "Open scenario"
      extensionFilters.add(
        new FileChooser.ExtensionFilter("JSON files", "*.json")
      )
    Option(chooser.showOpenDialog(stage)).map(file => os.Path(file.toPath))

  override def showError(message: String): Unit =
    val alert = new Alert(Alert.AlertType.Error):
      title = "Cannot load scenario"
      headerText = "The selected scenario could not be loaded."
      contentText = message
    alert.initOwner(stage)
    alert.showAndWait()
    ()
