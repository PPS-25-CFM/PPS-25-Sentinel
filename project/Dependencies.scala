// format: off
import sbt.*

/** Contains the project dependencies.
  */
object Dependencies {
  object Modules {
    /*
     * Scalatest
     */
    lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.2.20"
    /*
     * Mockito
     */
    lazy val mockito = "org.scalatestplus" %% "mockito-5-23" % "3.2.20.0"
    /*
     * Scalafx 
     */
    lazy val scalaFXVersion = "22.0.0-R33"
    lazy val scalafx = "org.scalafx" %% "scalafx" % scalaFXVersion
    /*
     * OpenJFX modules
     */
    lazy val fxVersion = scalaFXVersion.split('.').head
    lazy val oss = Seq("linux", "win", "mac-aarch64")
    lazy val modules = Seq("base", "graphics", "controls", "media", "fxml", "web")
    lazy val fxDependencies =
      for {
        module <- modules
        os <- oss
      } yield  "org.openjfx" % s"javafx-$module" % fxVersion classifier os
    /*
     * Monix 
     */
    lazy val monix = "io.monix" %% "monix" % "3.4.0"
    /* 
     * uPickle
     */
    lazy val uPickle = "com.lihaoyi" %% "upickle" % "3.1.0"
    /* 
     * os-lib
     */
    lazy val osLib = "com.lihaoyi" %% "os-lib" % "0.10.7"
  }
  import Modules.*
  lazy val reactive: Seq[ModuleID] = Seq(monix)
  /** GUI dependencies. 
   */
  lazy val gui: Seq[ModuleID] = scalafx +: fxDependencies
  /** A sequence of testing dependencies.
   */
  lazy val testing: Seq[ModuleID] = Seq(scalaTest, mockito) map (_ % Test)
  /** Serialization dependencies
    */
  lazy val serialization: Seq [ModuleID] = Seq(uPickle, osLib)
}
// format: on
