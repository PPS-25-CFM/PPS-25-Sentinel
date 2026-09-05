package it.unibo.sentinel.control.serialization

import it.unibo.sentinel.control.serialization.Codec.Validation
import scala.util.Try

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
  def save(model: M): Either[Validation, Unit]

  /** Loads a domain model instance associated with the specified key.
    *
    * @param key
    *   the unique identifier of the domain model to retrieve.
    * @return
    *   `Right(M)` containing the loaded model if found and valid, or
    *   `Left(Validation)` if the operation fails or the model is invalid.
    */
  def load(key: Key): Either[Validation, M]

object FileRepository:

  /** Standard sentinel folder in the root directory
    */
  final val folderPath: os.Path = os.home / ".sentinel"

  extension (path: String) def inRoot: os.Path = folderPath / path

/** Repository that uses the file system to store and load data.
  */
final class FileRepository[M: Codec](using
    extension: String,
    idExtractor: M => String
) extends Repository[String, M]:

  override def save(model: M): Either[Validation, Unit] =
    val data = summon[Codec[M]].encode(model)
    val fileName = s"${idExtractor(model)}$extension"
    writeToFile(data, fileName)

  override def load(fileName: String): Either[Validation, M] =
    readFromFile(fileName).flatMap:
      summon[Codec[M]].decode(_)

  private def writeToFile(
      data: String,
      fileName: String
  ): Either[Validation, Unit] =
    operate(fileName) { path =>
      os.makeDir.all(FileRepository.folderPath)
      os.write.over(path, data)
    }(Validation.FileAlreadyExists.apply)

  private def readFromFile(fileName: String): Either[Validation, String] =
    val correctName: String =
      if fileName.endsWith(extension) then fileName else s"$fileName$extension"
    operate(correctName)(path => os.read(path)):
      Validation.FileNotFound.apply

  private def operate[A](fileName: String)(operation: os.Path => A)(
      validation: String => Validation
  ): Either[Validation, A] =
    import it.unibo.sentinel.control.serialization.FileRepository.inRoot
    val targetPath = fileName.inRoot
    Try {
      operation(targetPath)
    }.toEither.left.map { _ =>
      validation(fileName)
    }
