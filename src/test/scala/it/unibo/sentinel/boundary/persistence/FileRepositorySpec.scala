package it.unibo.sentinel.boundary.persistence

import it.unibo.sentinel.UnitTest
import org.mockito.Mockito
import it.unibo.sentinel.boundary.serialization.Codec
import it.unibo.sentinel.boundary.serialization.Codec.Validation

class FileRepositorySpec extends UnitTest:

  private val testNumber: Int = 123
  private val extension: String = "txt"
  given codec: Codec[Int] = Mockito.mock(classOf[Codec[Int]])
  Mockito
    .when(codec.encode(testNumber))
    .thenReturn(testNumber.toString())
  Mockito
    .when(codec.decode(testNumber.toString()))
    .thenReturn(Right(testNumber))
  private val repo = new FileRepository[Int](extension)

  private def withTemporaryFile(test: os.Path => Unit): Unit =
    val dir = os.temp.dir()
    val file = dir / s"test_file.$extension"
    try test(file)
    finally os.remove.all(dir)

  "A FileRepository" when:

    "writing a file" should:

      "create a new one when it doesn't exist" in withTemporaryFile: file =>
        os.exists(file) shouldBe false
        repo.save(testNumber, file) shouldBe Right(())
        os.exists(file) shouldBe true
        os.read(file) shouldBe testNumber.toString

      "overwrite the existing one" in withTemporaryFile: file =>
        os.write(file, "old_content")
        repo.save(testNumber, file) shouldBe Right(())
        os.read(file) shouldBe testNumber.toString

    "reading a file" should:

      "read the correct content" in withTemporaryFile: file =>
        repo.save(testNumber, file)
        repo.load(file) shouldBe Right(testNumber)

      "return FileNotFound if the file does not exist" in withTemporaryFile:
        file =>
          val fakePath = file / os.up / s"fake_file.$extension"
          repo.load(fakePath) shouldBe Left(
            Validation.FileNotFound(fakePath.toString)
          )
