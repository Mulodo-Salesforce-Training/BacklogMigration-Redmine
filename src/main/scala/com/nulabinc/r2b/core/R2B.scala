package com.nulabinc.r2b.core

import java.util.Locale

import com.nulabinc.backlog.importer.actor.backlog.BacklogActor
import com.nulabinc.backlog.importer.core.BacklogConfig
import com.nulabinc.r2b.actor.convert.ConvertActor
import com.nulabinc.r2b.actor.redmine.RedmineActor
import com.nulabinc.r2b.actor.utils.R2BLogging
import com.nulabinc.r2b.cli.{ExecuteCommand, InitCommand, ParamProjectKey, ParameterValidator}
import com.nulabinc.r2b.conf.{ConfigBase, R2BConfig}
import com.nulabinc.r2b.utils.{ClassVersion, DisableSSLCertificateCheckUtil}
import com.osinka.i18n.{Lang, Messages}
import org.rogach.scallop._

import scalax.file.Path

class CommandLineInterface(arguments: Seq[String]) extends ScallopConf(arguments) {

  implicit val userLang = if (Locale.getDefault.equals(Locale.JAPAN)) Lang("ja") else Lang("en")

  version(ConfigBase.NAME + " " + ConfigBase.VERSION + " (c) nulab.inc")

  banner(
    """Usage: Backlog Migration for Redmine [OPTION]....
      | """.stripMargin)
  footer("\n " + Messages("help"))

  val help = opt[String]("help", descr = Messages("help.show_help"))
  val version = opt[String]("version", descr = Messages("help.show_version"))

  val execute = new Subcommand("execute") {
    val backlogKey = opt[String]("backlog.key", descr = Messages("help.backlog.key"), required = true, noshort = true)
    val backlogUrl = opt[String]("backlog.url", descr = Messages("help.backlog.url"), required = true, noshort = true)
    val redmineKey = opt[String]("redmine.key", descr = Messages("help.redmine.key"), required = true, noshort = true)
    val redmineUrl = opt[String]("redmine.url", descr = Messages("help.redmine.url"), required = true, noshort = true)

    val projects = opt[List[String]]("projects", descr = Messages("help.projects"), required = true)
    val importOnly = opt[Boolean]("importOnly", descr = Messages("help.importOnly"), required = true)
  }

  val init = new Subcommand("init") {
    val backlogKey = opt[String]("backlog.key", descr = Messages("help.backlog.key"), required = true, noshort = true)
    val backlogUrl = opt[String]("backlog.url", descr = Messages("help.backlog.url"), required = true, noshort = true)
    val redmineKey = opt[String]("redmine.key", descr = Messages("help.redmine.key"), required = true, noshort = true)
    val redmineUrl = opt[String]("redmine.url", descr = Messages("help.redmine.url"), required = true, noshort = true)

    val projects = opt[List[String]]("projects", descr = Messages("help.projects"), required = true)
  }
}

object R2B extends R2BLogging {

  def main(args: Array[String]) {
    if (ClassVersion.isValid()) {
      try {
        val cli: CommandLineInterface = new CommandLineInterface(args)
        execute(cli)
        System.exit(0)
      } catch {
        case e: Throwable ⇒
          e.printStackTrace()
          error(e)
          System.exit(1)
      }
    } else {
      info(Messages("requirements_java8", System.getProperty("java.specification.version")))
      System.exit(1)
    }
  }

  private def execute(cli: CommandLineInterface) = {
    DisableSSLCertificateCheckUtil.disableChecks()
    cli.subcommand match {
      case Some(cli.execute) =>
        val r2bConf: R2BConfig = new R2BConfig(
          cli.execute.backlogUrl(), cli.execute.backlogKey(),
          cli.execute.redmineUrl(), cli.execute.redmineKey(),
          cli.execute.projects().map(new ParamProjectKey(_)))

        showTitle()

        if (isParameterValid(r2bConf)) {
          if(cli.execute.importOnly()) {
            showImportStart()
            BacklogActor(BacklogConfig(r2bConf.backlogUrl, r2bConf.backlogKey))
            showImportFinish()
          } else {

            val r2bRoot: Path = Path.fromString(ConfigBase.R2B_ROOT)
            r2bRoot.deleteRecursively(force = true, continueOnFailure = true)

            val executeCommand: ExecuteCommand = new ExecuteCommand(r2bConf)
            if (executeCommand.check()) {
              val useProjects: Seq[ParamProjectKey] = executeCommand.useProjectsConfirm()
              val conf: R2BConfig = r2bConf.copy(projects = useProjects)
              if (executeCommand.confirm(useProjects)) {

                showImportStart()

                RedmineActor(conf)
                ConvertActor(conf)
                BacklogActor(BacklogConfig(conf.backlogUrl, conf.backlogKey))

                showImportFinish()

              } else showImportCancel()
            } else showImportUncompleted()
          }
        } else showImportUncompleted()
      case Some(cli.init) =>

        val r2bConf: R2BConfig = new R2BConfig(
          cli.execute.backlogUrl(), cli.execute.backlogKey(),
          cli.execute.redmineUrl(), cli.execute.redmineKey(),
          cli.execute.projects().map(new ParamProjectKey(_)))

        showTitle()

        if (isParameterValid(r2bConf)) {
          val initCommand: InitCommand = new InitCommand(r2bConf)
          initCommand.execute()
        }

      case _ =>
        newLine()
        title(ConfigBase.NAME + " " + ConfigBase.VERSION + " (c) nulab.inc", TOP)
        info(Messages("help.sample_command"))
        info(Messages("help"))
    }
  }

  private def showTitle() = {
    newLine()
    title(ConfigBase.NAME + " " + ConfigBase.VERSION + " (c) nulab.inc", TOP)
  }

  private def showImportStart() = {
    newLine()
    title(Messages("start"), TOP)
    separatorln()
  }

  private def showImportFinish() = {
    newLine()
    title(Messages("finish"), BOTTOM)
  }

  private def showImportCancel() = {
    newLine()
    separator()
    info(Messages("cancel"))
  }

  private def showImportUncompleted() = {
    newLine()
    title(Messages("import_uncompleted"), BOTTOM)
  }

  private def isParameterValid(r2bConf: R2BConfig): Boolean = {
    val validator: ParameterValidator = new ParameterValidator(r2bConf)
    val errors: Seq[String] = validator.validate()
    if (errors.nonEmpty) {
      newLine()
      info(Messages("mapping.show_parameter_error"))
      separator()
      errors.foreach(info(_))
      separator()
      false
    } else true
  }

}

