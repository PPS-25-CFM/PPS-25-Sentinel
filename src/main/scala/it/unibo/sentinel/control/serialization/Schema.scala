package it.unibo.sentinel.control.serialization

import it.unibo.sentinel.control.serialization.Codec.Validation

/** Intermediate model between `Sentinel`'s domain and an external serialization
  * tool.
  */
trait Schema:

  /** Validates the schema.
    *
    * @return
    *   an `Either` containing a [[Validation]] error on `Left`, or the valid
    *   schema on `Right`
    */
  def validated: Either[Validation, Schema]

extension (schemas: Seq[Schema])
  private def validateAll: Either[Validation, Unit] =
    schemas.iterator
      .map(_.validated)
      .collectFirst { case Left(err) => err }
      .toLeft(())
