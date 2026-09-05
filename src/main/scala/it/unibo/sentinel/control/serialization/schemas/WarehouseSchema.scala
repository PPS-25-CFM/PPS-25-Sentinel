package it.unibo.sentinel.control.serialization.schemas

import it.unibo.sentinel.control.serialization.Schema
import it.unibo.sentinel.core.warehouse.Warehouse
import it.unibo.sentinel.control.serialization.Codec.Validation
import it.unibo.sentinel.control.serialization.converters.PositionConverter
import it.unibo.sentinel.core.warehouse.Tile
import it.unibo.sentinel.control.serialization.Codec.validate
import it.unibo.sentinel.control.serialization.validateAll

/** Schema of a [[Tile]].
  */
enum TileSchema extends Schema:

  case Floor(cost: Int)

  override def validated: Either[Validation, TileSchema] = this match
    case Floor(cost) if cost < 0 =>
      Left(Validation.TileValidation(Tile.Validation.NegativeCost(cost)))
    case _ => Right(this)

/** Schema of a [[Warehouse]].
  */
case class WarehouseSchema(
    id: String,
    width: Int,
    height: Int,
    tiles: Seq[(PositionSchema, TileSchema)]
) extends Schema:

  override def validated: Either[Validation, WarehouseSchema] =
    for
      _ <- tiles.map(_._2).validateAll
      _ <- validateSize
      _ <- validateBounds
    yield this

  private def validateSize: Either[Validation, Unit] =
    validate(width > 0 && height > 0):
      Validation.WarehouseValidation:
        Warehouse.Validation.InvalidSize(width, height)

  private def validateBounds: Either[Validation, Unit] =
    val converted = tiles.map(p => PositionConverter.toDomain(p._1)).toList
    converted.collectFirst { case Left(err) => err } match
      case Some(validationError) => Left(validationError)
      case None                  =>
        val positions = converted.collect { case Right(pos) => pos }
        val outOfBounds = positions.filter: pos =>
          pos.x < 0 || pos.x >= width || pos.y < 0 || pos.y >= height
        validate(outOfBounds.isEmpty):
          Validation.WarehouseValidation:
            Warehouse.Validation.TilesOutOfBounds(outOfBounds, width, height)
