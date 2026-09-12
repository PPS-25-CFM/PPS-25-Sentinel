package it.unibo.sentinel.boundary.gui.fx

import scalafx.Includes.observableList2ObservableBuffer
import scalafx.scene.{Node, Scene}
import scalafx.scene.control.Button
import scalafx.scene.layout.FlowPane

/** Common controls for the warehouse views. */
private[fx] object FxControls:
  private val stylesheet = Option(
    getClass.getResource("/it/unibo/sentinel/boundary/gui/warehouse.css")
  )
    .map(_.toExternalForm)

  def style(scene: Scene): Scene =
    stylesheet.foreach(url => scene.stylesheets += url)
    scene

  def button(label: String, action: () => Unit): Button =
    new Button(label):
      styleClass += "warehouse-button"
      stylesheet.foreach(url => stylesheets += url)
      delegate.setOnAction(_ => action())

  def backToMenu(dismiss: () => Unit): Button =
    button("Back to menu", dismiss)

  /** Wrapping keeps every action reachable when the window is narrow. */
  def toolbar(controls: Seq[Node]): FlowPane =
    new FlowPane:
      styleClass += "warehouse-toolbar"
      minWidth = 0
      children = controls

  def zoomControls(
      zoomIn: () => Unit,
      zoomOut: () => Unit,
      zoomToFit: () => Unit
  ): Seq[Button] =
    Seq(button("−", zoomOut), button("+", zoomIn), button("Fit", zoomToFit))
