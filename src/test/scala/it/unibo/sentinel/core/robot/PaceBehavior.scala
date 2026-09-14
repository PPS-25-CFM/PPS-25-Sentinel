package it.unibo.sentinel.core.robot

import it.unibo.sentinel.UnitTest
import it.unibo.sentinel.core.routing.Path
import it.unibo.sentinel.core.simulation.Tick

trait PaceBehavior extends RobotFixture:
  self: UnitTest =>

  def pacedRobot(build: => Robot, pace: Tick): Unit =

    "following a path with its pace" should:

      "slow every step cost by its pace" in:
        val robot = build
        robot.follow(path)
        robot.path.value shouldBe path.slowed(pace)

      "keep the same positions" in:
        val robot = build
        robot.follow(path)
        robot.path.value.positions shouldBe positions

      "leave an empty path empty" in:
        val robot = build
        robot.follow(Path.empty)
        robot.path.value shouldBe Path.empty
