package it.unibo.sentinel.control

import it.unibo.sentinel.control.Engine.BasicEngine
import it.unibo.sentinel.core.simulation.Simulation
import monix.execution.Scheduler
import org.mockito.Mockito.*
import scala.concurrent.duration.*

class BasicEngineSpec extends EngineSpecBehaviour:

  override def createEngine(simulation: Simulation, period: FiniteDuration)(
      using Scheduler
  ): Engine =
    BasicEngine(simulation, period)

  "A BasicEngine" when:

    "controlled" should:

      "not react to commands" in new EngineFixture:
        engine.start()
        engine.next()
        engine.back()
        engine.resume()
        engine.pause()
        scheduler.tick()
        verify(simulation, times(1)).step()
