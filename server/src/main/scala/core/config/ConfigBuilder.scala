package core.config

import java.io.PrintStream

import better.files.File
import com.martiansoftware.nailgun.NGContext
import core.FsbtUtil.stripQuotes
import core.config.FsbtProject.Variables
import core.dependencies.DependencyResolver._
import core.dependencies.{DependencyResolver, MavenDependency, MavenDependencyScope}

import scala.util.{Failure, Success}

object ConfigBuilder {

  def build(workDir: String): FsbtProject = stage0(workDir, System.out)

  def build(context: NGContext): FsbtProject = stage0(context.getWorkingDirectory, context.out)

  private def stage0(workDir: String, out: PrintStream) = {
    val configFilePath = workDir + "/build.fsbt"
    stage1(parseConfigFile(configFilePath), configFilePath, workDir, out)
  }

  private def parseConfigFile(path: String) = {
    ConfigDSL.parseConfigFile(path) match {
      case Success(configValues) => configValues
      case Failure(ex) => throw ex
    }
  }

  private def stage1(configEntries: List[Any], configFilePath: String, workDir: String, out: PrintStream) = {
    val variables = configEntries.collect { case Variable(key: String, value: String) => (key, value) }.toMap
    val dependencies = resolveAll(configEntries.collect { case DependencyList(deps) => deps }.flatten.map(parseDependency(_, variables)))

    val name = if (variables.contains("name")) {
      variables("name")
    } else {
      throw new ConfigFileValidationException("must contain a \"name\" variable")
    }

    val environment: Environment.Value = if (System.getProperty("os.name").contains("Windows")) {
      Environment.Windows
    } else {
      Environment.Unix
    }

    val modules = getModules(workDir, variables, dependencies, environment, out,
      configEntries.collect{ case Modules(moduleList) => moduleList.map(stripQuotes)}.flatten)

    FsbtProject(dependencies, workDir, File(workDir + "/target/"), name, environment, variables, modules)
  }

  private def getModules(workDir: String,
                         variables: Variables,
                         deps: List[MavenDependency],
                         environment: Environment.Value,
                         out: PrintStream,
                         modulesList: List[String]) : List[FsbtProject] = {
    modulesList.map{
      module =>
        val moduleWorkDir = s"$workDir/$module/"
        val configFilePath = moduleWorkDir + "build.fsbt"
        if(File(configFilePath).notExists) {
          out.println(s"""Module \"$module\" is invalid""")
        }

        val configEntries = parseConfigFile(configFilePath)
        val allVariables = variables ++ configEntries.collect { case Variable(key: String, value: String) => (key, value) }.toMap
        val dependencies = deps ++ resolveAll(configEntries.collect { case DependencyList(d) => d }.flatten.map(parseDependency(_, allVariables)))
        val modules = getModules(workDir, allVariables, dependencies, environment, out,
          configEntries.collect{ case Modules(moduleList) => moduleList.map(stripQuotes)}.flatten)

        FsbtProject(dependencies, workDir, File(workDir + "/target/"), module, environment, allVariables, modules)
    }
  }

  private def resolveVariable(x: ValueOrVariable, variables: Map[String, String]) = {
    x match {
      case Value(rawValue) => rawValue
      case VariableCall(key) =>
        if (variables.contains(key))
          variables(key)
        else
          throw new ConfigFileValidationException(s"""does not have a \"$key\" variable"""")
    }
  }

  private def parseDependency(dep: Dependency, variables: Map[String, String]) = {
    val artifact = stripQuotes(resolveVariable(dep.artifact, variables))
    val group = stripQuotes(resolveVariable(dep.group, variables))
    val version = stripQuotes(resolveVariable(dep.version, variables))
    val withScalaDeps = if (dep.withScalaVersion == "%%") true else false
    val scope = if (dep.scope.isDefined) {
      MavenDependencyScope.withName(stripQuotes(resolveVariable(dep.scope.get, variables)))
    } else MavenDependencyScope.Compile
    new MavenDependency(group, artifact, version, false, withScalaDeps, scope)
  }


  //    val config: Map[ConfigEntry.Value, Any] = (for {
  //      _ <- ConfigValidator.validateConfigFileExists(configFilePath)
  //      configMap <- ConfigDSL.parseConfigFile(configFilePath)
  //    } yield buildConfig(configMap, workDir)).get
  //


  //    new FsbtConfig(
  //      config(ConfigEntry.dependencyList).asInstanceOf[List[Dependency]].map(new MavenDependency(_)),
  //      File(config(ConfigEntry.targetDirectory).toString),
  //      config(ConfigEntry.workingDir).toString, config(ConfigEntry.name).toString,
  //      environment)


  //  val withScalaVersion = if (scalaVer.length == 2) true else false
  //  val scope = if(scope0.isDefined){
  //    scope0.get._2
  //  }else{
  //    "compile"
  //  }


  // TODO make this actually make sense
  //  private def buildConfig(configMap: Map[ConfigEntry.Value, ConfigValue], workDir: String): Map[ConfigEntry.Value, Any] = {
  //
  //    val defaultConfig = Map(
  //      (ConfigEntry.workingDir, workDir),
  //      (ConfigEntry.sourceDirectory, workDir + "/src/"),
  //      (ConfigEntry.targetDirectory, workDir + "/target/"),
  //      (ConfigEntry.version, "1.0"),
  //      (ConfigEntry.name, ""),
  //      (ConfigEntry.dependencyList, "")
  //    )
  //
  //    defaultConfig.map((keyValue) => {
  //      val key = keyValue._1
  //      if (configMap.keySet.contains(key)) {
  //        configMap(key) match {
  //          case PureString(value) => (key, value)
  //          case DependencyList(list) => (key, list)
  //          case Modules(list) => (key, list)
  //        }
  //      } else {
  //        keyValue
  //      }
  //    })
  //  }
}

