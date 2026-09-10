package it.unibo.sentinel.boundary.gui.fx

import it.unibo.sentinel.boundary.gui.fx.FxUtils.onFx
import it.unibo.sentinel.boundary.gui.fx.FxUtils.{defaultWidth, defaultHeight}
import it.unibo.sentinel.boundary.gui.toolkit.{
  Menu,
  SimulationView,
  StatisticsView,
  Toolkit,
  Window
}
import monix.eval.Task
import scalafx.application.Platform

/** Toolkit implementation using the fx library
  */
object FxToolkit extends Toolkit:

  Platform.startup(() => ())

  override type V = FxView

  override val window: Window[FxView] =
    new FxWindow(Some(defaultWidth), Some(defaultHeight))

  override def menu: V & Menu = new FxMenuView

  override def statistics: V & StatisticsView = new FxStatisticsView

  override def simulation: V & SimulationView = new FxSimulationView

  override def shutdown(): Task[Unit] = onFx(Platform.exit())
