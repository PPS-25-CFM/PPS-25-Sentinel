package it.unibo.sentinel.boundary.gui.fx

import it.unibo.sentinel.boundary.gui.toolkit.MenuView
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.Scene
import scalafx.scene.control.{Button, Label}
import scalafx.scene.layout.VBox

/** Presents the application's available activities. */
final class FxMenuView extends FxView with MenuView:
  private val runButton = button("Run Simulation")

  override lazy val scene: Scene = new Scene(new VBox:
    alignment = Pos.Center
    spacing = 16
    padding = Insets(32)
    style = "-fx-background-color: #0F172A;"
    children = Seq(
      new Label("Sentinel"):
        style = "-fx-text-fill: #F8FAFC; -fx-font-size: 32px;"
      ,
      button("Edit Warehouse", unavailable = true),
      button("Edit Scenario", unavailable = true),
      runButton
    ))

  override def onRunSimulation(action: () => Unit): Unit =
    runButton.onAction() = _ => action()

  private def button(text: String, unavailable: Boolean = false): Button =
    new Button(text):
      disable = unavailable
      prefWidth = 240
      prefHeight = 44
