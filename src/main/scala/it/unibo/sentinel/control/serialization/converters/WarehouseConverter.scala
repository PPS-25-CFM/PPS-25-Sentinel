package it.unibo.sentinel.control.serialization.converters

import it.unibo.sentinel.control.serialization.Converter
import it.unibo.sentinel.control.serialization.schemas.TileSchema
import it.unibo.sentinel.control.serialization.Codec.Validation
import it.unibo.sentinel.control.serialization.schemas.WarehouseSchema
import it.unibo.sentinel.core.warehouse.Warehouse
import it.unibo.sentinel.core.warehouse.Tile
import it.unibo.sentinel.core.warehouse.value
import it.unibo.sentinel.core.warehouse.WarehouseId
import it.unibo.sentinel.core.simulation.Tick

object WarehouseConverter extends Converter[Warehouse, WarehouseSchema]:

  given Converter[Tile, TileSchema]:

    override def toSchema(model: Tile): TileSchema = model match
      case Tile.Floor(cost) => TileSchema.Floor(cost.value)

    override def toDomain(schema: TileSchema): Either[Validation, Tile] =
      schema match
        case TileSchema.Floor(cost) => Right(Tile.Floor(Tick(cost)))

  override def toSchema(model: Warehouse): WarehouseSchema =
    val tilesMap = model.tiles.map: (pos, tile) =>
      val posSchema = PositionConverter.toSchema(pos)
      val tileSchema = summon[Converter[Tile, TileSchema]].toSchema(tile)
      (posSchema, tileSchema)
    WarehouseSchema(model.id.value, model.width, model.height, tilesMap)

  override def toDomain(
      schema: WarehouseSchema
  ): Either[Validation, Warehouse] =
    var warehouse =
      Warehouse.empty(WarehouseId(schema.id), schema.width, schema.height)
    for
      (posSchema, tileSchema) <- schema.tiles
      pos <- PositionConverter.toDomain(posSchema)
      tile <- summon[Converter[Tile, TileSchema]].toDomain(tileSchema)
    do warehouse = warehouse.withTile(pos)(tile)
    Right(warehouse)
