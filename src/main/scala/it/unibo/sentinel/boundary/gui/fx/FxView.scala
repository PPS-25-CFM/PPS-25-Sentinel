package it.unibo.sentinel.boundary.gui.fx

import scalafx.scene.Scene

/** View based on the fx library
  */
abstract class FxView:

  /** @return
    *   the fx [[Scene]] used to render the view on the [[FxWindow]]
    */
  def scene: Scene
