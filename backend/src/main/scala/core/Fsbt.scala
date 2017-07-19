package core

import better.files.File
import com.martiansoftware.nailgun.NGContext
import com.typesafe.scalalogging.Logger
import context.ContextUtil
import core.config._
import core.dependencies.MavenDependency
import org.slf4j.LoggerFactory
import sbt.util
import xsbti.compile.ZincCompilerUtil

//import sbt.inc.ZincUtils
import sbt.internal.inc.ScalaInstance
import xsbti.compile.{DependencyChanges, Output}

import scala.sys.process._
import scala.util.matching.Regex

object Fsbt {

  val logger = Logger(LoggerFactory.getLogger(this.getClass))

  def compile(args: List[String], config: FsbtConfig): Unit = {

    val deps = config.dependencies

    logger.debug("Classpath: ")

    deps.foreach(f => logger.debug(f.jarFile.path.toString))

    val scalaSourceFiles = config.getScalaSourceFiles
    val javaSourceFiles = config.getJavaSourceFiles

    config.target.createIfNotExists(asDirectory = true)

    logger.debug("Compiling scala...")
    val compileScala = List("scalac", "-cp", config.classPath) ++ scalaSourceFiles ++ List("-d", config.target.toJava.getAbsolutePath)





    val t0 = System.nanoTime()
    val scalaOutput = compileScala.!!
    val t1 = System.nanoTime()
    logger.debug(s"Elapsed: ${(t1 - t0)/1000000} ms")

    logger.debug("Compiling java...")
    val compileJava = List("javac", "-cp", config.classPath) ++ javaSourceFiles ++ List("-d", config.target.toJava.getAbsolutePath)
    val t2 = System.nanoTime()
    val output = compileJava.!!
    val t3 = System.nanoTime()
    logger.debug(s"Elapsed: ${(t3 - t2)/1000000} ms")
  }

  def run(args: List[String], config: FsbtConfig): Unit = {

    val ctx = ContextUtil.identifyContext(config.getTargetClasses)
    println(ctx)
    if (ctx.isEmpty) {
      println("No context were found")
    } else {
      ctx.head.run(config.target)
    }
  }

  def test(config: FsbtConfig): Unit = {

    val targetClasses = config.getTargetClasses.map(_.toString())
    val command = List("java",  "-cp", config.classPath + "/home/humblehound/Dev/fsbt/testProject/target/test/java/TestJunit.class") ++ List("org.junit.runner.JUnitCore", "test.java.TestJunit")
    println(command)
    val output = command.lineStream
    output.foreach(println)
  }

  def clean(config: FsbtConfig): Unit = {
    for (file <- config.target.list) {
      file.delete()
    }
  }

  def nailMain(context: NGContext): Unit = {

    val config = ConfigBuilder.build(context)
    val args = context.getArgs.toList

    if (args.isEmpty) {
      println("Printing info")
    } else
    args.foreach {
      case "compile" => compile(args, config)
      case "test" => test(config)
      case "run" =>
        compile(args, config)
        run(args, config)
      case "clean" => clean(config)
      case unknown => println("command not found: " + unknown)
    }
  }

}
