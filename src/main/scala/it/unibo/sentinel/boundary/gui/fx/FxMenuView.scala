package it.unibo.sentinel.boundary.gui.fx

import it.unibo.sentinel.boundary.gui.fx.FxUtils.onFx
import it.unibo.sentinel.boundary.gui.toolkit.{MenuCommand, Menu}
import it.unibo.sentinel.control.serialization.Codec.Validation
import monix.eval.Task
import monix.execution.Scheduler
import monix.reactive.Observable
import monix.reactive.subjects.ConcurrentSubject
import scalafx.Includes.observableList2ObservableBuffer
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.Scene
import scalafx.scene.control.{Alert, Button, Label, TextField}
import scalafx.scene.control.Alert.AlertType
import scalafx.scene.layout.VBox
import scalafx.stage.{FileChooser, Stage}

/** */
final class FxMenuView extends FxView with Menu:
  import Scheduler.Implicits.global

  private val subject = ConcurrentSubject.publish[MenuCommand]

  private val chooser = new FileChooser:
    delegate.setTitle("Choose a scenario")
    extensionFilters += new FileChooser.ExtensionFilter("Scenario", "*.json")

  override def commands: Observable[MenuCommand] = subject

  override lazy val scene: Scene = new Scene(new VBox:
    alignment = Pos.Center
    spacing = 20
    padding = Insets(48)
    style = "-fx-background-color: #0F172A;"
    children = Seq(
      heading("Sentinel"),
      editWarehouse,
      pending("Configure Scenario"),
      runSimulation
    ))

  override def report(failure: Validation): Task[Unit] = onFx:
    val alert = new Alert(AlertType.Error):
      delegate.setTitle("Scenario not loaded")
      headerText = "The chosen scenario could not be loaded"
      contentText = describe(failure)
    val _ = alert.showAndWait()

  private lazy val runSimulation: Button =
    new Button("Run Simulation"):
      style = enabledStyle
      delegate.setOnAction: _ =>
        val owner = FxMenuView.this.scene.window()
        Option(chooser.showOpenDialog(owner)).foreach: file =>
          val _ = subject.onNext(MenuCommand.RunSimulation(os.Path(file)))

  private lazy val editWarehouse: Button =
    new Button("Edit Warehouse"):
      style = enabledStyle
      delegate.setOnAction: _ =>
        showNewWarehouseDialog()

  /** Opens a small modal asking for the id/width/height of a new [[Warehouse]],
    * emitting [[MenuCommand.NewWarehouse]] on confirmation.
    */
  private def showNewWarehouseDialog(): Unit =
    val idField = new TextField:
      promptText = "Warehouse id"
    val widthField = new TextField:
      promptText = "Width"
    val heightField = new TextField:
      promptText = "Height"
    val create = new Button("Create"):
      disable = true
      style = enabledStyle

    def valid: Boolean =
      idField.text.value.trim.nonEmpty &&
        widthField.text.value.toIntOption.exists(_ > 0) &&
        heightField.text.value.toIntOption.exists(_ > 0)

    Seq(idField, widthField, heightField).foreach: field =>
      field.text.onChange { (_, _, _) => create.disable = !valid }

    val dialog = new Stage:
      title = "New Warehouse"
    dialog.scene = new Scene(new VBox:
      spacing = 12
      padding = Insets(24)
      style = "-fx-background-color: #0F172A;"
      children = Seq(idField, widthField, heightField, create))

    create.delegate.setOnAction: _ =>
      val _ = subject.onNext(
        MenuCommand.NewWarehouse(
          idField.text.value.trim,
          widthField.text.value.toInt,
          heightField.text.value.toInt
        )
      )
      dialog.close()

    dialog.show()

  private def pending(text: String): Button =
    new Button(text):
      disable = true
      style = disabledStyle

  private def heading(text: String): Label =
    new Label(text):
      style = "-fx-text-fill: #F8FAFC; -fx-font-size: 32px;"

  private def describe(failure: Validation): String = failure match
    case Validation.FileNotFound(path)      => s"File not found: $path"
    case Validation.FileAlreadyExists(name) => s"File already exists: $name"
    case Validation.Syntax(error)           => s"Malformed JSON: $error"
    case Validation.WarehouseValidation(e)  => s"Invalid warehouse: $e"
    case Validation.TileValidation(e)       => s"Invalid tile: $e"
    case Validation.MissionValidation(e)    => s"Invalid mission: $e"
    case Validation.ScenarioValidation(e)   => s"Invalid scenario: $e"
    case Validation.ItemValidation(e)       => s"Invalid item: $e"

  private val enabledStyle: String =
    "-fx-background-color: #1E293B; -fx-text-fill: #F8FAFC;" +
      " -fx-background-radius: 8; -fx-padding: 14 32 14 32;" +
      " -fx-font-size: 16px;"

  private val disabledStyle: String =
    "-fx-background-color: #1E293B; -fx-text-fill: #475569;" +
      " -fx-background-radius: 8; -fx-padding: 14 32 14 32;" +
      " -fx-font-size: 16px; -fx-opacity: 1;"
