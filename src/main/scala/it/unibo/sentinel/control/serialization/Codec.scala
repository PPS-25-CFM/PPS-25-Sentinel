package it.unibo.sentinel.control.serialization

import it.unibo.sentinel.core.warehouse.Warehouse
import it.unibo.sentinel.core.warehouse.Tile
import it.unibo.sentinel.core.mission.Mission

/** A type class or trait representing a contract for converting domain models
  * into a string-based representation (e.g., JSON, XML, or custom format).
  *
  * @tparam Model
  *   the type of the entity to be encoded
  */
trait Encoder[Model]:

  /** Encodes the provided model instance into its string representation.
    *
    * @param model
    *   the instance of [[Model]] to encode
    * @return
    *   the encoded string representation of the model
    */
  def encode(model: Model): String

/** A trait representing a contract for parsing a string-based representation
  * (such as JSON, XML, or custom format) into a domain model instance.
  *
  * @tparam Model
  *   the type of the domain entity to produce upon successful decoding
  */
trait Decoder[Model]:

  /** Decodes a string input into a domain model instance.
    *
    * @param input
    *   the string representation to be decoded
    * @return
    *   an [[scala.util.Left]] containing a [[SerializationError]] if decoding
    *   fails, or a [[scala.util.Right]] containing the decoded [[Model]] on
    *   success
    */
  def decode(input: String): Either[Codec.Validation, Model]

/** A unified interface for bidirectional data conversion, combining both
  * [[Encoder]] and [[Decoder]] capabilities for a specific domain model.
  *
  * A `Codec` defines a symmetric contract capable of serializing a domain
  * object into a string representation and deserializing that string back into
  * the domain model.
  *
  * @tparam Model
  *   the type of the domain entity to encode and decode
  */
trait Codec[Model] extends Encoder[Model], Decoder[Model]

object Codec:

  /** Represents an error that can occur during serialization and/or
    * deserialization.
    */
  enum Validation:
    /** The file was not found.
      */
    case FileNotFound(path: String)

    /** There is a file with the same name in the destination.
      */
    case FileAlreadyExists(name: String)

    /** Generic syntax (parsing) error.
      */
    case Syntax(error: String)

    /** Warehouse generation error.
      */
    case WarehouseValidation(error: Warehouse.Validation)

    /** Tile generation error.
      */
    case TileValidation(error: Tile.Validation)

    /** Mission generation error.
      */
    case MissionValidation(error: Mission.Validation)

    /** Scenario generation error.
      */
    case ScenarioValidation(error: it.unibo.sentinel.core.scenario.Validation)

  def validate(condition: Boolean)(
      error: Validation
  ): Either[Validation, Unit] =
    Either.cond(condition, (), error)
