package it.unibo.sentinel.boundary.gui.fx

import monix.execution.CancelablePromise
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.control.Button
import scalafx.scene.layout.HBox

/** */
private[fx] object FxControls:

  /** @param exit
    */
  def backToMenu(exit: CancelablePromise[Unit]): HBox =
    val button = new Button("Back to menu"):
      style = "-fx-background-color: #1E293B; -fx-text-fill: #F8FAFC;" +
        " -fx-background-radius: 6; -fx-padding: 6 14 6 14;"
      delegate.setOnAction: _ =>
        val _ = exit.trySuccess(())
    new HBox:
      alignment = Pos.CenterLeft
      padding = Insets(12)
      style = "-fx-background-color: #0F172A;"
      children = Seq(button)
