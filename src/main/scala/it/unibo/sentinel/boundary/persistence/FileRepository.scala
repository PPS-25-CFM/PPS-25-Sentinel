package it.unibo.sentinel.boundary.persistence

import it.unibo.sentinel.boundary.serialization.Codec
import it.unibo.sentinel.boundary.serialization.Codec.Validation
import scala.util.Try

object FileRepository:

  /** Standard sentinel folder in the root directory.
    */
  final val folderPath: os.Path = os.home / ".sentinel"

/** Repository that uses the file system to store and load data.
  */
final class FileRepository[M: Codec](extension: String)
    extends Repository[os.Path, M]:

  override def save(model: M, path: os.Path): Either[Validation, Unit] =
    val correct = correctPath(path)
    val data = summon[Codec[M]].encode(model)
    tryOperation(os.write.over(correct, data)):
      Validation.FileAlreadyExists(correct.toString)

  override def load(path: os.Path): Either[Validation, M] =
    val correct = correctPath(path)
    for
      data <- readFile(correct)
      result <- summon[Codec[M]].decode(data)
    yield result

  private def readFile(path: os.Path): Either[Validation, String] =
    tryOperation(os.read(path)):
      Validation.FileNotFound(path.toString)

  private def tryOperation[A](operation: => A)(
      validation: Validation
  ): Either[Validation, A] =
    Try(operation).toEither.left.map(_ => validation)

  private def correctPath(path: os.Path): os.Path =
    if path.ext == extension then path
    else path / os.up / s"${path.last}.$extension"
