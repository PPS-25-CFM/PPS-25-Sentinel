package it.unibo.sentinel.control

import it.unibo.sentinel.boundary.gui.toolkit.WarehouseView
import it.unibo.sentinel.control.serialization.FileRepository
import it.unibo.sentinel.core.item.Item
import it.unibo.sentinel.core.simulation.Tick
import it.unibo.sentinel.core.warehouse.{Position, Tile, Warehouse, WarehouseId}

import scala.util.{Failure, Success, Try}

/** */
trait WarehouseManager:
  /** @param at
    * @param cost
    */
  def putFloor(at: Position, cost: Int): Unit

  /** @param at
    * @param item
    */
  def putShelf(at: Position, item: Item): Unit

  /** @param at
    * @param cost
    */
  def putLoadingBay(at: Position, cost: Int): Unit

  /** @param at
    */
  def removeTile(at: Position): Unit

  /** */
  def save(using repo: FileRepository[Warehouse]): Unit

object WarehouseManager:
  def apply(name: String, width: Int, height: Int)(
      view: WarehouseView
  ): WarehouseManager =
    new WarehouseManager:
      private val id = WarehouseId(name)
      private var warehouse = Warehouse.empty(id, width, height)

      /** @param at
        * @param cost
        */
      override def putFloor(at: Position, cost: Int): Unit =
        Try(Tick(cost)) match
          case Success(time) =>
            warehouse = warehouse.withTile(at)(Tile.Floor(time))
            view.render(warehouse)
          case Failure(_) => ()

      /** @param at
        * @param item
        */
      override def putShelf(at: Position, item: Item): Unit =
        warehouse = warehouse.withTile(at)(Tile.Shelf(item))
        view.render(warehouse)

      /** @param at
        * @param cost
        */
      override def putLoadingBay(at: Position, cost: Int): Unit =
        Try(Tick(cost)) match
          case Success(time) =>
            warehouse = warehouse.withTile(at)(Tile.LoadingBay(time))
            view.render(warehouse)
          case Failure(_) => ()

      /** @param at
        */
      override def removeTile(at: Position): Unit =
        warehouse = warehouse.withoutTile(at)
        view.render(warehouse)

      /** */
      override def save(using repo: FileRepository[Warehouse]): Unit =
        repo.save(warehouse)
