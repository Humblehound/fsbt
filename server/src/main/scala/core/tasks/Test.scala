package core.tasks

import com.martiansoftware.nailgun.NGContext
import com.typesafe.scalalogging.LazyLogging
import core.FsbtUtil
import core.config.{Environment, FsbtProject}
import core.dependencies.MavenDependency

import scala.sys.process._
import scala.util.matching.Regex

object Test extends Task with LazyLogging {


  def getClasspath(config: FsbtProject) = config.dependencies.foldRight("")((dep, res) => dep.jarFile.path.toAbsolutePath.toString + Environment.pathSeparator(config.environment) + res) + "."

  override def perform(config: FsbtProject)(implicit ctx: NGContext): Unit = {
    val scalaTest = List("java",  "-cp", getClasspath(config)) ++ List("org.scalatest.tools.Runner", "-R", s"${config.target}", "-o")
    println(scalaTest)
    val output = scalaTest.!
  }

}
