package it.unibo.sentinel.boundary.gui.fx

import it.unibo.sentinel.boundary.gui.toolkit.Toolkit
import it.unibo.sentinel.boundary.gui.toolkit.Snapshot
import it.unibo.sentinel.boundary.gui.fx.FxUtils.onFx
import it.unibo.sentinel.boundary.gui.fx.panels.WarehousePanel
import it.unibo.sentinel.boundary.gui.fx.FxUtils.defaultWidth
import it.unibo.sentinel.boundary.gui.fx.FxUtils.defaultHeight
import scalafx.scene.Scene
import scalafx.scene.layout.BorderPane
import scalafx.application.Platform
import it.unibo.sentinel.boundary.gui.fx.panels.MissionsPanel

/** Toolkit implementation using the fx library
  */
object FxToolkit extends Toolkit:

  Platform.startup(() => ())

  override type W = FxWindow
  override type V[Model] = FxView[Model]

  override val window: W = new FxWindow(Some(defaultWidth), Some(defaultHeight))

  override def simulation: V[Snapshot] = new FxView[Snapshot]:

    private val root = new BorderPane

    override def scene: Scene = new Scene(root)

    override def render(model: Snapshot): Unit = onFx:
      root.center = new WarehousePanel(model._1)
      root.left = new MissionsPanel(model._2)
      window.resize()
