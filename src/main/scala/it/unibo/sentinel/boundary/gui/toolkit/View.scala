package it.unibo.sentinel.boundary.gui.toolkit

/** Represents a UI responsible for visualizing a given model
  */
trait View[Model]:

  /** Loads all the graphics components to visualize the given model
    *
    * @param model
    *   the current state to display
    */
  def render(model: Model): Unit
