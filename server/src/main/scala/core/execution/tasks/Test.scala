package core.execution.tasks

import ch.qos.logback.classic.Logger
import com.martiansoftware.nailgun.NGContext
import com.typesafe.scalalogging.LazyLogging
import core.FsbtUtil
import core.config.compile.ExecutionConfig
import core.config.{Environment, FsbtModule}
import core.execution.Task

import scala.concurrent.{Await, Future}
import scala.sys.process._
import scala.util.{Failure, Success}
import scala.util.matching.Regex
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

case class Test() extends Task {

  def specsRegex = "".r
  def TestRegex = "".r

  def getClasspath(config: FsbtModule) =
    config.dependencies
      .foldRight("")((dep, res) =>
        dep.jarFile.path.toAbsolutePath.toString +
          Environment.pathSeparator(config.environment) +
          res) + "."

  // TODO fix concurrent testing - weird future execution on subsequent runs
  def perform(config: FsbtModule)(implicit ctx: NGContext, logger: Logger): Unit = {
//    logger.debug(config.modules.length.toString)
//    val future = config.modules.map(x => Future(testModule(x)))
//    val f = Await.result(Future.sequence(future), Duration.Inf)
//    testModule(config)
//    config.modules.map(testModule)
    testModule(config)
  }

  def testModule(config: FsbtModule)(implicit logger: Logger): Int = {
    logger.debug(s"Starting test of ${config.target}")
    try{
      val classPath = getClasspath(config)
      val potentialTests = FsbtUtil.recursiveListFiles(config.target.pathAsString, Test.specRegex)
      if(potentialTests.size > 0 && config.dependencies.exists(p => p.groupId == "org.scalatest" && p.artifactId == "scalatest")){
        val scalaTest = List("java", "-cp", classPath) ++ List("org.scalatest.tools.Runner", "-o", "-R", s"${config.target}")
        val process = scalaTest.run
        process.exitValue()
      }else{
        logger.debug("No tests found")
        0
      }
    }catch{
      case ex: Exception => logger.debug("FUCK", ex)
        1
    }
  }

  override def perform(module: FsbtModule,config: ExecutionConfig, moduleTaskCompleted: FsbtModule => Unit)(implicit ctx: NGContext, logger: Logger): Unit =
  {

  }
}

object Test{
   val specRegex: Regex = ".*Spec.class$".r
}

