package it.unibo.sentinel.boundary.persistence

import it.unibo.sentinel.boundary.serialization.Codec.Validation

/** A repository interface for persisting and retrieving domain models.
  *
  * @tparam Key
  *   the type of the unique identifier used to retrieve models.
  * @tparam M
  *   the type of the domain model managed by this repository.
  */
trait Repository[Key, M]:

  /** Persists a domain model instance to storage.
    *
    * @param model
    *   the domain model instance to save.
    * @param key
    *   the key used to save the model.
    */
  def save(model: M, key: Key): Either[Validation, Unit]

  /** Loads a domain model instance associated with the specified key.
    *
    * @param key
    *   the unique identifier of the domain model to retrieve.
    * @return
    *   `Right(M)` containing the loaded model if found and valid, or
    *   `Left(Validation)` if the operation fails or the model is invalid.
    */
  def load(key: Key): Either[Validation, M]
