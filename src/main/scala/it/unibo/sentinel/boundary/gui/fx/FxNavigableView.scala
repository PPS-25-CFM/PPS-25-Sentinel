package it.unibo.sentinel.boundary.gui.fx

import it.unibo.sentinel.boundary.gui.toolkit.NavigableView
import scalafx.scene.control.Button

/** Shares the menu action between navigable ScalaFX views. */
abstract class FxNavigableView extends FxView with NavigableView:
  protected lazy val menuButton: Button = new Button("Back to Menu")

  final override def onMenu(action: () => Unit): Unit =
    menuButton.onAction() = _ => action()
