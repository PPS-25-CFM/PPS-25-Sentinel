package it.unibo.sentinel.core.collisions

import it.unibo.sentinel.UnitTest
import it.unibo.sentinel.core.mission.{Mission, MissionId, Priority}
import it.unibo.sentinel.core.robot.{Robot, RobotId}
import it.unibo.sentinel.core.routing.{Path, Step}
import it.unibo.sentinel.core.scenario.Placement
import it.unibo.sentinel.core.simulation.Tick
import it.unibo.sentinel.core.warehouse.Position

trait CollisionHandlerBehavior:
  this: UnitTest =>

  protected given SelectionPolicy = new SelectionPolicy:
    override def select(robots: Seq[Robot]): Option[RobotId] =
      robots.headOption.map(_.id)

  protected def createMovingRobot(id: String, target: Position): Robot =
    val robot = Robot.drone(RobotId(id))
    robot.accept(
      Mission.relocate(
        MissionId(s"m-$id"),
        target,
        Tick(10),
        Priority.normal
      )
    )
    robot.follow(Path(Step(target, Tick.unit)))
    robot.tick()
    robot

  def correctCollisionResolver(handler: CollisionHandler): Unit =

    "resolving indirect collisions" should:

      "allow the selected winner to move" in:
        val r1 = createMovingRobot("R1", Position(1, 1))
        val r2 = createMovingRobot("R2", Position(1, 1))
        val p1 = Placement(r1, Position(0, 0))
        val p2 = Placement(r2, Position(0, 1))
        val actions = handler.resolveCollisions(Seq(p1, p2))
        actions(r1.id) shouldBe Action.Move
        actions(r2.id) shouldNot be(Action.Move)

      "prevent contenders from moving if a stationary robot occupies the target cell" in:
        val r1 = createMovingRobot("R1", Position(1, 1))
        val r2 = createMovingRobot("R2", Position(1, 1))
        val r3Stationary = Robot.drone(RobotId("R3"))
        val p1 = Placement(r1, Position(0, 0))
        val p2 = Placement(r2, Position(0, 1))
        val p3Stationary = Placement(r3Stationary, Position(1, 1))
        val actions = handler.resolveCollisions(Seq(p1, p2, p3Stationary))
        actions(r1.id) shouldNot be(Action.Move)
        actions(r2.id) shouldNot be(Action.Move)

      "ignore stationary robots and allow moving robots to proceed to empty cells" in:
        val r1Stationary = Robot.drone(RobotId("R1_stat"))
        val r2 = createMovingRobot("R2", Position(1, 1))
        val p1Stationary = Placement(r1Stationary, Position(0, 0))
        val p2 = Placement(r2, Position(0, 1))
        val actions = handler.resolveCollisions(Seq(p1Stationary, p2))
        actions.get(r1Stationary.id) shouldBe None
        actions(r2.id) shouldBe Action.Move

    "handling chain dependencies" should:

      "cascade non-move decisions when a robot cannot move into an occupied cell" in:
        val r1 = createMovingRobot("R1", Position(1, 0))
        val r2 = createMovingRobot("R2", Position(2, 0))
        val r3 = createMovingRobot("R3", Position(3, 0))
        val p1 = Placement(r1, Position(0, 0))
        val p2 = Placement(r2, Position(1, 0))
        val p3 = Placement(r3, Position(2, 0))
        val p4 = Placement(Robot.drone(RobotId("R4")), Position(3, 0))
        val actions = handler.resolveCollisions(Seq(p1, p2, p3, p4))
        actions(r1.id) shouldNot be(Action.Move)
        actions(r2.id) shouldNot be(Action.Move)
        actions(r3.id) shouldNot be(Action.Move)
