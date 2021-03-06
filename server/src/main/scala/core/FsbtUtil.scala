package core

import better.files.File
import core.config.FsbtModule
import core.dependencies.{MavenDependency, MavenDependencyScope}

import scala.annotation.tailrec
import scala.util.matching.Regex

object FsbtUtil {

  def recursiveListFiles(path: String, r: Regex): List[File] = {
    val these = File(path).listRecursively
    these.filter(f => r.findFirstIn(f.name).isDefined).toList
  }

  def stripQuotes(string: String): String = string.replaceAll("^\"|\"$", "")

  // do not use
//  def getNestedDependencies(config: FsbtModule, scope: MavenDependencyScope.Value = MavenDependencyScope.Compile): List[MavenDependency] = {
//    @tailrec
//    def getDependenciesRec(queue: List[FsbtModule], acc: List[MavenDependency]): List[MavenDependency] = queue match {
//      case Nil => acc
//      case head::tail => getDependenciesRec(tail ++ head.modules, config.dependencies.filter(_.scope == scope) ++ acc)
//    }
//    getDependenciesRec(config :: Nil, List())
//  }
}
