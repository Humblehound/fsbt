package core.config

import better.files.File
import core.dependencies.{DependencyResolver, MavenDependency}

import scala.util.matching.Regex

/**
  * Created by humblehound on 21.07.17.
  */
class FsbtConfig(rawDependencies: List[MavenDependency], val target: File, workingDir: String, val projectName: String, val os: String) extends EnvironmentSpecific{

  val scalaRegex = new Regex(".scala$")
  val javaRegex = new Regex(".java$")
  // parse only top-level class files, omit nested classes
  val classRegex = new Regex("^[^$]+.class$")

  implicit val environment: Environment.Value = os.contains("Windows") match{
    case true => Environment.Windows
    case false => Environment.Unix
  }

  def recursiveListFiles(path: String, r: Regex): List[File] = {
    val these = File(path).listRecursively
    these.filter(f => r.findFirstIn(f.name).isDefined).toList
  }

  val scalaSourceDir = workingDir + s"${dirSeparator}src${dirSeparator}main${dirSeparator}scala"
  val javaSourceDir = workingDir + s"${dirSeparator}src${dirSeparator}main${dirSeparator}java"
  val scalaTestSourceDir = workingDir + s"${dirSeparator}src${dirSeparator}test${dirSeparator}scala"
  val javaTestSourceDir = workingDir + s"${dirSeparator}src${dirSeparator}test${dirSeparator}java"

  def getScalaSourceFiles: List[File] = recursiveListFiles(workingDir, scalaRegex)
  def getJavaSourceFiles: List[File] = recursiveListFiles(workingDir, javaRegex)


  def getClasspath = dependencies.foldRight("")((dep, res) => dep.jarFile.path.toAbsolutePath.toString + pathSeparator + res) + "."
  def getTestClassPath = dependencies.foldRight("")((dep, res) => dep.jarFile.path.toAbsolutePath.toString + pathSeparator + res) + s"$target$dirSeparator"


  def getTargetClasses = recursiveListFiles(target.toString(), classRegex)
  def getTestClasses = recursiveListFiles(s"$target", classRegex)
  def getAllTargetClasses = recursiveListFiles(s"$target", new Regex(".*\\.class"))

  val dependencies: List[MavenDependency] = new DependencyResolver(rawDependencies).resolveAll()

}

object FsbtConfig {
  val fsbtPath = System.getProperty("user.home") + "/.fsbt"
  val fsbtCache = s"$fsbtPath/cache"
  val zincCache = s"$fsbtPath/cache/compileCache"
  val scalaVersion = "_2.12"
}


