package it.unibo.sentinel.boundary.gui.fx

import it.unibo.sentinel.boundary.gui.toolkit.View
import scalafx.scene.Scene

/** View based on the fx library
  */
abstract class FxView[Model] extends View[Model]:

  /** @return
    *   the fx [[Scene]] used to render the view on the [[FxWindow]]
    */
  def scene: Scene
