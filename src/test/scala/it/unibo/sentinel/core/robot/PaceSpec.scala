package it.unibo.sentinel.core.robot

import it.unibo.sentinel.UnitTest
import it.unibo.sentinel.core.item.Item
import it.unibo.sentinel.core.mission.{Mission, MissionId}
import it.unibo.sentinel.core.routing.Path
import it.unibo.sentinel.core.simulation.Tick
import it.unibo.sentinel.core.warehouse.Position

class PaceSpec extends UnitTest with RobotFixture:

  /** Minimal probe isolating the Pace mixin from BaseRobot logic. */
  private class Probe(val id: RobotId) extends Robot:
    var stored: Option[Path] = None
    override def mission: Option[MissionId] = None
    override def status: RobotStatus = RobotStatus.Idle
    override def canAccept(mission: Mission): Boolean = true
    override def accept(mission: Mission): Unit = ()
    override def release(): Unit = stored = None
    override def clearRoute(): Unit = stored = None
    override def follow(path: Path): Unit = stored = Some(path)
    override def path: Option[Path] = stored
    override def next: Option[Position] = None
    override def remaining: Tick = Tick.zero
    override def step(): Unit = ()
    override def pause(): Unit = ()
    override def resume(): Unit = ()
    override def tick(): Unit = ()
    override def pick(item: Item): Boolean = false
    override def drop(item: Item): Option[Item] = None

  private def paced(pace: Tick): Probe & Pace =
    new Probe(robotId) with Pace(pace)

  "Pace presets" should:
    "be ordered from fast to slow" in:
      Pace.fast.value shouldBe 0
      (Pace.fast.value < Pace.normal.value) shouldBe true
      (Pace.normal.value < Pace.slow.value) shouldBe true

    "have fast as zero extra cost" in:
      Pace.fast shouldBe Tick.zero

  "A Paced robot" when:
    "following a path" should:
      "add its pace to every step cost" in:
        val robot = paced(Tick(2))
        robot.follow(path)
        robot.stored.value.remaining shouldBe (costs.headOption.value + Tick(2))

      "leave positions unchanged" in:
        val robot = paced(Pace.slow)
        robot.follow(path)
        robot.stored.value.positions shouldBe positions

      "leave an empty path empty" in:
        val robot = paced(Pace.slow)
        robot.follow(Path.empty)
        robot.stored.value shouldBe Path.empty

      "leave the path unchanged when pace is fast" in:
        val robot = paced(Pace.fast)
        robot.follow(path)
        robot.stored.value shouldBe path

  "Factory robots" should:
    "expose their pace through the waiting time" in:
      val drone = Robot.drone(robotId)
      val light = Robot.lightCarrier(robotId)
      val heavy = Robot.heavyCarrier(robotId)
      drone.follow(path)
      light.follow(path)
      heavy.follow(path)
      drone.remaining shouldBe costs.headOption.value + Pace.fast
      light.remaining shouldBe costs.headOption.value + Pace.normal
      heavy.remaining shouldBe costs.headOption.value + Pace.slow
