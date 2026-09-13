package it.unibo.sentinel.boundary.launcher

import it.unibo.sentinel.boundary.gui.fx.FxToolkit
import monix.execution.Scheduler.Implicits.global
import it.unibo.sentinel.boundary.persistence.Repository
import it.unibo.sentinel.core.warehouse.Warehouse
import it.unibo.sentinel.boundary.persistence.FileRepository
import it.unibo.sentinel.core.scenario.Scenario
import it.unibo.sentinel.boundary.serialization.Codec.Validation
import it.unibo.sentinel.boundary.serialization.JsonSerialization.given

trait Configuration:
  val extension = "json"

  given Repository[os.Path, Warehouse] =
    new FileRepository[Warehouse](extension)

  given (String => Either[Validation, Warehouse]) =
    (warehouseId: String) =>
      summon[Repository[os.Path, Warehouse]]
        .load(FileRepository.folderPath / warehouseId)

  given Repository[os.Path, Scenario] =
    new FileRepository[Scenario](extension)

/** Application launcher.
  *
  * Runs the [[Application]] on the fx [[Toolkit]], keeping the main thread
  * alive until the user closes the window.
  */
object Launcher extends Configuration:
  def main(args: Array[String]): Unit =
    Application(FxToolkit)
      .start()
      .runSyncUnsafe()
