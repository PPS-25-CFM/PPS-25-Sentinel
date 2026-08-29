package it.unibo.sentinel.core.scenario

import it.unibo.sentinel.core.warehouse.{Warehouse, Position}
import it.unibo.sentinel.core.robot.{Robot, RobotId}
import it.unibo.sentinel.core.mission.{Mission, MissionId}
import it.unibo.sentinel.core.scenario.Policies.Routing
import it.unibo.sentinel.core.scenario.Policies.Assignment
import it.unibo.sentinel.core.simulation.Environment
import it.unibo.sentinel.core.scenario.Policies.CollisionAvoidance
import it.unibo.sentinel.core.scenario.Policies.CollisionSelection
import scala.collection.immutable.ListMap
import it.unibo.sentinel.core.simulation.Tick

/** Represents the intention of a [[Robot]] to move to a specific [[Position]]
  *
  * @param robotId
  *   the [[Robot]]'s id
  * @param position
  *   the destination
  */
case class Intent(robotId: RobotId, position: Position)

/** Represents a [[Robot]] placed in a [[Position]] in the [[Warehouse]].
  *
  * @param robot
  *   the [[Robot]] to place in the [[Warehouse]].
  * @param at
  *   the [[Position]] where to place the [[Robot]].
  */
final case class Placement(robot: Robot, at: Position):

  /** @returns
    *   an intent to where the robot wants to move
    */
  def intent: Intent =
    (robot.next, robot.remaining) match
      case (Some(pos), Tick.zero) => Intent(robot.id, pos)
      case _                      => Intent(robot.id, at)

/** Represents a description of a [[Robot]] to spawn in a [[Scenario]]. It will
  * be used to create a [[Robot]] in the given [[Position]] when the
  * [[Scenario]] is started.
  *
  * @param id
  *   the [[RobotId]] of the [[Robot]] to spawn.
  * @param at
  *   the [[Position]] where to spawn the [[Robot]].
  */
final case class Spawn(id: RobotId, at: Position):
  /** @return
    *   the [[Placement]] of the [[Robot]] to spawn in the [[Warehouse]].
    */
  def toPlacement: Placement = Placement(Robot(id), at)

/** */
enum Validation:
  /** @param position
    *   the [[Position]] that is already occupied by another [[Robot]].
    */
  case PositionOccupied(position: Position)

  /** @param position
    *   the [[Position]] that is not a floor tile.
    */
  case NotFloorTile(position: Position)

  /** @param id
    *   the [[RobotId]] of the [[Robot]] that is already exists.
    */
  case RobotAlreadyExists(id: RobotId)

  /** @param id
    *   the [[MissionId]] of the [[Mission]] that is already exists.
    */
  case MissionAlreadyExists(id: MissionId)

/** Represents the dynamic context of the environment to simulate.
  */
trait Scenario:
  /** @return
    *   the [[Warehouse]] the [[Scenario]] refers to.
    */
  def warehouse: Warehouse

  /** @return
    *   the [[Policies.Routing]] policy of the [[Scenario]].
    */
  def routing: Policies.Routing

  /** @return
    *   the [[Policies.Assignment]] policy of the [[Scenario]].
    */
  def assignment: Policies.Assignment

  /** @return
    *   the [[Policies.CollisionSelection]] policy of the [[Scenario]].
    */
  def collisionSelection: Policies.CollisionSelection

  /** @return
    *   the [[Policies.CollisionAvoidance]] policy of the [[Scenario]].
    */
  def collisionAvoidance: Policies.CollisionAvoidance

  /** @param routing
    *   the [[Policies.Routing]] policy to use in the new [[Scenario]].
    * @return
    *   a new [[Scenario]] with the given [[Policies.Routing]] policy.
    */
  def withRouting(routing: Policies.Routing): Scenario

  /** @param routing
    *   the [[Policies.Assignment]] policy to use in the new [[Scenario]].
    * @return
    *   a new [[Scenario]] with the given [[Policies.Assignment]] policy.
    */
  def withAssignment(assignment: Policies.Assignment): Scenario

  /** @param collisionSelection
    *   the [[Policies.CollisionSelection]] policy to use in the new
    *   [[Scenario]].
    * @return
    *   a new [[Scenario]] with the given [[Policies.CollisionSelection]]
    *   policy.
    */
  def withCollisionSelection(collisionSelection: CollisionSelection): Scenario

  /** @param collisionAvoidance
    *   the [[Policies.CollisionAvoidance]] policy to use in the new
    *   [[Scenario]].
    * @return
    *   a new [[Scenario]] with the given [[Policies.CollisionAvoidance]]
    *   policy.
    */
  def withCollisionAvoidance(collisionAvoidance: CollisionAvoidance): Scenario

  /** @return
    *   the [[Spawn]]s of the [[Scenario]].
    */
  def spawns: Seq[Spawn]

  /** @return
    *   the [[Mission]]s of the [[Scenario]].
    */
  def missions: Seq[Mission]

  /** @param spawn
    *   the [[Spawn]] to place in the [[Scenario]].
    * @return
    *   an [[Either]] containing the updated [[Scenario]] if the placement is
    *   valid, or a [[Validation]] error otherwise.
    */
  def place(spawn: Spawn): Either[Validation, Scenario]

  /** @param mission
    *   the [[Mission]] to load in the [[Scenario]].
    * @return
    *   an [[Either]] containing the updated [[Scenario]] if the loading is
    *   valid, or a [[Validation]] error otherwise.
    */
  def load(mission: Mission): Either[Validation, Scenario]

  /** Builds and initializes the simulation [[Environment]] from this
    * [[Scenario]].
    *
    * @return
    *   a new [[Environment]] populated with the current [[Warehouse]], spawned
    *   [[Robot]]s as [[Placement]]s, and loaded [[Mission]]s.
    */
  private[core] def build: Environment

object Scenario:
  import Validation.*

  /** @param warehouse
    *   the [[Warehouse]] the [[Scenario]] refers to.
    * @return
    *   a new [[Scenario]] with no robots nor missions for the given
    *   [[Warehouse]].
    */
  def in(warehouse: Warehouse): Scenario =
    Blueprint(
      warehouse,
      Seq.empty,
      Seq.empty,
      Routing.Distance,
      Assignment.Nearest,
      CollisionSelection.Random,
      CollisionAvoidance.Wait
    )

  private final case class Blueprint(
      warehouse: Warehouse,
      spawns: Seq[Spawn],
      missions: Seq[Mission],
      routing: Policies.Routing,
      assignment: Policies.Assignment,
      collisionSelection: Policies.CollisionSelection,
      collisionAvoidance: Policies.CollisionAvoidance
  ) extends Scenario:

    override def build: Environment =
      Environment(
        warehouse = warehouse,
        fleet = ListMap(spawns.map(s => s.id -> s.toPlacement)*),
        board = ListMap(missions.map(m => m.id -> m)*)
      )

    override def place(spawn: Spawn): Either[Validation, Scenario] =
      for
        _ <- ensure(
          warehouse.isTraversable(spawn.at),
          NotFloorTile(spawn.at)
        )
        _ <- ensure(
          !spawns.exists(_.at == spawn.at),
          PositionOccupied(spawn.at)
        )
        _ <- ensure(
          !spawns.exists(_.id == spawn.id),
          RobotAlreadyExists(spawn.id)
        )
      yield copy(spawns = spawns :+ spawn)

    override def load(mission: Mission): Either[Validation, Scenario] =
      for _ <- ensure(
          !missions.exists(_.id == mission.id),
          MissionAlreadyExists(mission.id)
        )
      yield copy(missions = missions :+ mission)

    override def withRouting(routing: Routing): Scenario =
      copy(routing = routing)

    override def withAssignment(assignment: Assignment): Scenario =
      copy(assignment = assignment)

    override def withCollisionSelection(
        collisionSelection: CollisionSelection
    ): Scenario =
      copy(collisionSelection = collisionSelection)

    override def withCollisionAvoidance(
        collisionAvoidance: CollisionAvoidance
    ): Scenario =
      copy(collisionAvoidance = collisionAvoidance)

    private def ensure(
        cond: => Boolean,
        error: => Validation
    ): Either[Validation, Unit] =
      Either.cond(cond, (), error)
