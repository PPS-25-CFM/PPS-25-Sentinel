package it.unibo.sentinel.core.simulation

import it.unibo.sentinel.core.robot.{Robot, RobotId}
import it.unibo.sentinel.core.scenario.Placement
import it.unibo.sentinel.core.warehouse.Warehouse
import it.unibo.sentinel.core.mission.{
  Action,
  Mission,
  MissionId,
  MissionStatus
}
import it.unibo.sentinel.core.routing.Path
import it.unibo.sentinel.core.robot.RobotStatus
import it.unibo.sentinel.core.collisions.Action as CollisionAction

/** Provides query operations to inspect the state of the simulation.
  */
trait Queries:
  import it.unibo.sentinel.core.robot.{Robot, RobotStatus}

  /** @return
    *   the current [[Placement]]s of all robots in the simulation.
    */
  def placements: Seq[Placement]

  /** @return
    *   all [[Mission]]s present in the simulation.
    */
  def missions: Seq[Mission]

  /** @param robotId
    *   the [[RobotId]] of the robot to query.
    * @return
    *   the [[Robot]] with the given id, if any.
    */
  def robot(rid: RobotId): Option[Robot]

  /** @param mid
    *   the [[MissionId]] of the mission to query
    * @return
    *   the [[Mission]] with the given id, if any.
    */
  def mission(mid: MissionId): Option[Mission]

  /** @param robotId
    *   the [[RobotId]] of the robot contained in the placement to query.
    * @return
    *   the [[Placement]] with the given id, if any.
    */
  def placement(rid: RobotId): Option[Placement]

  /** @param status
    *   the [[RobotStatus]] to filter robots by.
    * @return
    *   the [[Placement]]s of robots that currently have the given
    *   [[RobotStatus]].
    */
  def standing(status: RobotStatus): Seq[Placement] =
    placements.filter(_.robot.status == status)

  /** @return
    *   all [[Mission]]s currently in [[Pending]] status.
    */
  def pendingMissions: Seq[Mission] =
    missions.filter(_.status == MissionStatus.Pending)

/** Represents the mutable simulation state, maintaining the [[Warehouse]]
  * layout, the robot [[fleet]] and the mission [[board]].
  *
  * @param warehouse
  *   the [[Warehouse]] where the simulation takes place.
  * @param fleet
  *   the map of current [[Placement]]s indexed by [[RobotId]].
  * @param board
  *   the map of active [[Mission]]s indexed by [[MissionId]].
  */
private[core] final class Environment private[core] (
    val warehouse: Warehouse,
    private var fleet: Map[RobotId, Placement],
    private var board: Map[MissionId, Mission]
) extends Queries:

  override def placements: Seq[Placement] = fleet.values.toSeq
  override def missions: Seq[Mission] = board.values.toSeq

  override def robot(rid: RobotId): Option[Robot] =
    fleet.get(rid).map(_.robot)

  override def mission(mid: MissionId): Option[Mission] =
    board.get(mid)

  override def placement(rid: RobotId): Option[Placement] =
    fleet.get(rid)

  /** @param rid
    * @param mid
    * @return
    *   an [[Event]] if the assignment was successful, None otherwise
    */
  def assign(rid: RobotId, mid: MissionId): Option[Event] =
    for
      spot <- fleet.get(rid)
      robot = spot.robot
      mission <- board.get(mid)
    yield
      robot.accept(mission)
      board = board + (mid -> mission.assignTo(rid))
      Event.MissionAssigned(rid, mid)

  /** @param rid
    * @param path
    * @return
    *   an [[Event]] if the routing was successful, None otherwise
    */
  def route(rid: RobotId, path: Path): Option[Event] =
    for
      spot <- fleet.get(rid)
      robot = spot.robot
    yield
      robot.follow(path)
      Event.RobotRouted(rid, path.positions)

  /** @param action
    *   action to execute
    * @return
    *   an [[Event]] if the action produces one
    */
  def execute(action: CollisionAction): Option[Event] =
    for
      spot <- fleet.get(action.id)
      robot = spot.robot
      event <- action match
        case CollisionAction.Block(id) if robot.status == RobotStatus.Moving =>
          robot.pause()
          Some(Event.RobotBlocked(id, spot.at))
        case CollisionAction.Unblock(id)
            if robot.status == RobotStatus.Waiting =>
          robot.resume()
          Some(Event.RobotUnblocked(id))
        case _ => None
    yield event

  /** @param r_id
    * @return
    *   an [[Event]] if the [[Robot]] was able to move, None otherwise
    */
  def advance(rid: RobotId): Option[Event] =
    for
      spot <- fleet.get(rid)
      robot = spot.robot
      from = spot.at
      intent = spot.intent
      if robot.status == RobotStatus.Moving && robot.remaining == Tick.zero
    yield
      robot.step()
      fleet += (rid -> spot.copy(at = intent.to))
      Event.RobotMoved(rid, from, intent.to)

  /** @param rid
    * @return
    *   the [[Event]]s produced by the action: [[Event.MissionCompleted]] if the
    *   mission reached [[Task.Done]] (preceded by [[Event.ItemPicked]] /
    *   [[Event.ItemDropped]] for deposit steps), [[Event.ItemPicked]] /
    *   [[Event.ItemDropped]] for intermediate deposit steps,
    *   [[Event.MissionFailed]] if failed, empty [[Seq]] otherwise
    */
  def perform(rid: RobotId): Seq[Event] =
    (for
      spot <- fleet.get(rid)
      robot = spot.robot
      mid <- robot.mission
      mission <- board.get(mid)
      action <- mission.currentAction
    yield action match
      case Action.Move(_) =>
        robot.release()
        board += (mid -> mission.complete)
        Seq(Event.MissionCompleted(mid))

      case Action.PickUp(item, at) =>
        if robot.pick(item) then
          val next = mission.completeCurrentAction
          board += (mid -> next)
          if next.isOver then robot.release() else robot.clearRoute()
          if next.isOver then
            Seq(
              Event.ItemPicked(rid, mid, item, at),
              Event.MissionCompleted(mid)
            )
          else Seq(Event.ItemPicked(rid, mid, item, at))
        else
          board += (mid -> mission.fail)
          releaseCarrier(mission)
          Seq(Event.MissionFailed(mid))

      case Action.Drop(item, at) =>
        robot.drop(item) match
          case Some(dropped) =>
            val next = mission.completeCurrentAction
            board += (mid -> next)
            if next.isOver then robot.release() else robot.clearRoute()
            if next.isOver then
              Seq(
                Event.ItemDropped(rid, mid, dropped, at),
                Event.MissionCompleted(mid)
              )
            else Seq(Event.ItemDropped(rid, mid, dropped, at))
          case None =>
            board += (mid -> mission.fail)
            releaseCarrier(mission)
            Seq(Event.MissionFailed(mid))
    ).toSeq.flatten

  /** Advances all missions in the environment by one step.
    *
    * @return
    *   the sequence of generated [[Event]]s (e.g. mission failures)
    */
  def tick(): Seq[Event] =
    placements.foreach(_.robot.tick())
    val events = for
      mission <- missions.filterNot(_.isOver)
      next = mission.tick
    yield
      board += (mission.id -> next)
      if next.status == MissionStatus.Failed then
        releaseCarrier(mission)
        Some(Event.MissionFailed(mission.id))
      else None

    events.flatten

  /** Releases the carrier of a mission if it exists.
    */
  private def releaseCarrier(mission: Mission): Unit =
    for
      rid <- mission.carrier
      carrier <- robot(rid)
    do carrier.release()

  /** Automatically fails all [[Mission]]s that are not over yet, releasing the
    * robots which were carrying them.
    *
    * @return
    *   a [[Seq]] of [[Event.MissionFailed]] for all missions that are not over
    *   yet.
    */
  def end: Seq[Event] =
    for
      mission <- missions.filterNot(_.isOver)
      next = mission.fail
    yield
      board += (mission.id -> next)
      releaseCarrier(mission)
      Event.MissionFailed(mission.id)

  /** @return
    *   a snapshot of the current state of the simulation.
    */
  def snapshot: Snapshot = Snapshot(
    warehouse = warehouse,
    robots = placements.map(spot =>
      RobotSnapshot(spot.robot.id, spot.robot.status, spot.at, spot.robot.path)
    ),
    missions = missions
  )
