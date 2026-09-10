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
import scalafx.scene.control.{Alert, Button, Label}
import scalafx.scene.control.Alert.AlertType
import scalafx.scene.layout.VBox
import scalafx.stage.FileChooser

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
      caption("Choose what to do"),
      pending("Edit Warehouse"),
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

  private def pending(text: String): Button =
    new Button(text):
      disable = true
      style = disabledStyle

  private def heading(text: String): Label =
    new Label(text):
      style = "-fx-text-fill: #F8FAFC; -fx-font-size: 32px;"

  private def caption(text: String): Label =
    new Label(text):
      style = "-fx-text-fill: #94A3B8; -fx-font-size: 16px;"

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
