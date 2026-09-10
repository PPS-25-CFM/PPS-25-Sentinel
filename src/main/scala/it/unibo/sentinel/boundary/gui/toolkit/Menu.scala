package it.unibo.sentinel.boundary.gui.toolkit

import it.unibo.sentinel.control.serialization.Codec.Validation
import monix.eval.Task

/** What the user can do in the menu.
  */
enum MenuCommand:
  /** @param scenario
    *   the path to the [[Scenario]] file to load and simulate.
    */
  case RunSimulation(scenario: os.Path)

/** Represents the menu of the application.
  */
trait Menu extends Interactive[MenuCommand]:
  /** @param failure
    *   the validation error to report.
    */
  def report(failure: Validation): Task[Unit]
