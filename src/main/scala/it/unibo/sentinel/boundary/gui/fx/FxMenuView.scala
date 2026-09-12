package it.unibo.sentinel.boundary.gui.fx

import it.unibo.sentinel.boundary.gui.fx.FxUtils.onFx
import it.unibo.sentinel.boundary.gui.toolkit.{MenuCommand, Menu}
import it.unibo.sentinel.control.serialization.Codec.Validation
import it.unibo.sentinel.control.serialization.FileRepository
import monix.eval.Task
import scalafx.Includes.observableList2ObservableBuffer
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.Scene
import scalafx.scene.control.{Alert, Button, Label, TextField}
import scalafx.scene.control.Alert.AlertType
import scalafx.scene.layout.VBox
import scalafx.stage.{FileChooser, Stage}
import scala.jdk.OptionConverters.*

/** */
final class FxMenuView extends FxView with Menu:

  private val scenarioChooser = chooser("Choose a scenario", "Scenario")

  private val warehouseChooser = chooser("Choose a warehouse", "Warehouse")

  override lazy val scene: Scene = new Scene(new VBox:
    alignment = Pos.Center
    spacing = 20
    padding = Insets(48)
    style = "-fx-background-color: #0F172A;"
    children = Seq(
      heading("Sentinel"),
      newWarehouse,
      openWarehouse,
      newScenario,
      openScenario,
      runSimulation
    ))

  override def report(failure: Validation): Task[Unit] = onFx:
    val alert = new Alert(AlertType.Error):
      delegate.setTitle("Operation failed")
      headerText = "The operation could not be completed"
      contentText = describe(failure)
    val _ = alert.showAndWait()

  private lazy val runSimulation: Button =
    new Button("Run Simulation"):
      style = enabledStyle
      delegate.setOnAction: _ =>
        choose(scenarioChooser).foreach: file =>
          emit(MenuCommand.RunSimulation(file))

  private lazy val newWarehouse: Button =
    new Button("New Warehouse"):
      style = enabledStyle
      delegate.setOnAction: _ =>
        showNewWarehouseDialog()

  private lazy val openWarehouse: Button =
    new Button("Open Warehouse"):
      style = enabledStyle
      delegate.setOnAction: _ =>
        choose(warehouseChooser).foreach: file =>
          emit(MenuCommand.OpenWarehouse(file))

  private lazy val newScenario: Button =
    new Button("New Scenario"):
      style = enabledStyle
      delegate.setOnAction: _ =>
        choose(warehouseChooser).foreach(showNewScenarioDialog)

  private lazy val openScenario: Button =
    new Button("Open Scenario"):
      style = enabledStyle
      delegate.setOnAction: _ =>
        choose(scenarioChooser).foreach: file =>
          emit(MenuCommand.OpenScenario(file))

  private def showNewScenarioDialog(warehouse: os.Path): Unit =
    val id = new TextField:
      promptText = "Scenario id"
    val hint = new Label("Choose a name for the scenario file.")
    val dialog =
      new javafx.scene.control.Dialog[javafx.scene.control.ButtonType]()
    val create = javafx.scene.control.ButtonType.OK
    dialog.initOwner(scene.window())
    dialog.setTitle("New Scenario")
    dialog.setHeaderText(s"Warehouse: ${warehouse.last}")
    dialog.getDialogPane.getButtonTypes
      .addAll(create, javafx.scene.control.ButtonType.CANCEL)
    dialog.getDialogPane.setContent(new VBox(8, id, hint).delegate)
    Option(dialog.getDialogPane.lookupButton(create))
      .foreach(_.setDisable(true))
    id.text.onChange { (_, _, _) =>
      val name = id.text.value.trim
      val valid = name.nonEmpty && !name.exists(c =>
        c.isControl || "/\\:*?\"<>|".contains(c)
      )
      val available = valid && !os.exists(warehouse / os.up / s"$name.json")
      hint.text = if !valid then "Enter a valid file name."
      else if !available then "A file with this name already exists."
      else s"Save as $name.json next to the warehouse."
      Option(dialog.getDialogPane.lookupButton(create))
        .foreach(_.setDisable(!available))
    }
    dialog.showAndWait().toScala.filter(_ == create).foreach { _ =>
      emit(MenuCommand.NewScenario(id.text.value.trim, warehouse))
    }

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
      emit(
        MenuCommand.NewWarehouse(
          idField.text.value.trim,
          widthField.text.value.toInt,
          heightField.text.value.toInt
        )
      )
      dialog.close()

    dialog.show()

  private def chooser(
      prompt: String,
      kind: String,
      initial: Option[os.Path] = Some(FileRepository.folderPath)
  ): FileChooser =
    new FileChooser:
      delegate.setTitle(prompt)
      extensionFilters += new FileChooser.ExtensionFilter(kind, "*.json")
      initial
        .filter(os.exists(_))
        .foreach: path =>
          initialDirectory = path.toIO

  private def choose(chooser: FileChooser): Option[os.Path] =
    val owner = FxMenuView.this.scene.window()
    Option(chooser.showOpenDialog(owner)).map(os.Path(_))

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
