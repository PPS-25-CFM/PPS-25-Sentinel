package it.unibo.sentinel.control.serialization

import it.unibo.sentinel.control.serialization.Codec.Validation

/** Converts between domain models and their corresponding serializable schemas.
  *
  * @tparam Model
  *   The domain model type.
  * @tparam ModelSchema
  *   The DTO/schema type used for serialization.
  */
trait Converter[Model, ModelSchema]:

  /** Converts a domain model to its schema representation.
    */
  def toSchema(model: Model): ModelSchema

  /** Reconstructs a domain model from its schema.
    */
  def toDomain(schema: ModelSchema): Either[Validation, Model]
