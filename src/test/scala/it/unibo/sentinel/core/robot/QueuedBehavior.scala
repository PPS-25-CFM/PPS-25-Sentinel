package it.unibo.sentinel.core.robot

import it.unibo.sentinel.UnitTest

trait QueuedBehavior extends RobotFixture:
  self: UnitTest =>

  def queuedRobot(build: Int => Robot, capacity: Int): Unit =

    "queuing missions up to capacity" should:

      "accept missions until the queue is full, then refuse" in:
        val robot = build(capacity)
        for _ <- 0 until capacity do
          robot.canAccept(mission1) shouldBe true
          robot.accept(mission1)
        robot.canAccept(mission1) shouldBe false

    "tracking workload" should:

      "grow with accepts and shrink with releases" in:
        val robot = build(capacity)
        robot.accept(mission1)
        robot.workload shouldBe Workload(1)
        robot.accept(mission2)
        robot.workload shouldBe Workload(2)
        robot.release()
        robot.workload shouldBe Workload(1)
        robot.release()
        robot.workload shouldBe Workload.zero

    "checking remaining capacity" should:

      "shrink with accepts and grow with releases" in:
        val robot = build(capacity)
        remainingOf(robot) shouldBe capacity
        robot.accept(mission1)
        remainingOf(robot) shouldBe capacity - 1
        robot.release()
        remainingOf(robot) shouldBe capacity

    "releasing without missions" should:

      "stay empty and idle" in:
        val robot = build(capacity)
        robot.release()
        robot.mission shouldBe None
        robot.workload shouldBe Workload.zero
        robot.status shouldBe RobotStatus.Idle

  private def remainingOf(robot: Robot): Int = robot match
    case queued: Queued => queued.remainingCapacity
    case _              => fail("expected a robot with queue")
