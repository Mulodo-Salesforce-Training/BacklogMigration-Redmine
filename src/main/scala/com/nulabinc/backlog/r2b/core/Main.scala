package com.nulabinc.backlog.r2b.core

import java.util.Locale

import com.nulabinc.backlog.migration.common.conf.{BacklogApiConfiguration, BacklogConfiguration}
import com.nulabinc.backlog.migration.common.utils.{ConsoleOut, Logging}
import com.nulabinc.backlog.r2b.cli.R2BCli
import com.nulabinc.backlog.r2b.conf._
import com.nulabinc.backlog.r2b.redmine.conf.RedmineApiConfiguration
import com.nulabinc.backlog.r2b.utils.{ClassVersion, DisableSSLCertificateCheckUtil}
import com.osinka.i18n.Messages
import org.fusesource.jansi.AnsiConsole
import org.rogach.scallop._
import spray.json._

class CommandLineInterface(arguments: Seq[String]) extends ScallopConf(arguments) with BacklogConfiguration with Logging {

  banner("""Usage: Backlog Migration for Redmine [OPTION]....
      | """.stripMargin)
  footer("\n " + Messages("cli.help"))

  val help    = opt[String]("help", descr = Messages("cli.help.show_help"))
  val version = opt[String]("version", descr = Messages("cli.help.show_version"))

  val execute = new Subcommand("execute") {
    val backlogKey = opt[String]("backlog.key", descr = Messages("cli.help.backlog.key"), required = true, noshort = true)
    val backlogUrl = opt[String]("backlog.url", descr = Messages("cli.help.backlog.url"), required = true, noshort = true)
    val redmineKey = opt[String]("redmine.key", descr = Messages("cli.help.redmine.key"), required = true, noshort = true)
    val redmineUrl = opt[String]("redmine.url", descr = Messages("cli.help.redmine.url"), required = true, noshort = true)

    val projectKey = opt[String]("projectKey", descr = Messages("cli.help.projectKey"), required = true)
    val importOnly = opt[Boolean]("importOnly", descr = Messages("cli.help.importOnly"), required = true)
    val optOut     = opt[Boolean]("optOut", descr = Messages("cli.help.optOut"), required = false)
    val help       = opt[String]("help", descr = Messages("cli.help.show_help"))
  }

  val init = new Subcommand("init") {
    val backlogKey = opt[String]("backlog.key", descr = Messages("cli.help.backlog.key"), required = true, noshort = true)
    val backlogUrl = opt[String]("backlog.url", descr = Messages("cli.help.backlog.url"), required = true, noshort = true)
    val redmineKey = opt[String]("redmine.key", descr = Messages("cli.help.redmine.key"), required = true, noshort = true)
    val redmineUrl = opt[String]("redmine.url", descr = Messages("cli.help.redmine.url"), required = true, noshort = true)

    val projectKey = opt[String]("projectKey", descr = Messages("cli.help.projectKey"), required = true)
    val help       = opt[String]("help", descr = Messages("cli.help.show_help"))
  }

  addSubcommand(execute)
  addSubcommand(init)

  verify()
}

object R2B extends BacklogConfiguration with Logging {

  def main(args: Array[String]) {
    ConsoleOut.println(s"""|${applicationName}
                 |--------------------------------------------------""".stripMargin)
    AnsiConsole.systemInstall()
    setLang()
    DisableSSLCertificateCheckUtil.disableChecks()
    checkRelease()
    if (ClassVersion.isValid()) {
      try {
        val cli: CommandLineInterface = new CommandLineInterface(args)
        execute(cli)
        AnsiConsole.systemUninstall()
        System.exit(0)
      } catch {
        case e: Throwable ⇒
          logger.error(e.getMessage, e)
          AnsiConsole.systemUninstall()
          System.exit(1)
      }
    } else {
      ConsoleOut.error(Messages("cli.require_java8", System.getProperty("java.specification.version")))
      AnsiConsole.systemUninstall()
      System.exit(1)
    }
  }

  private[this] def execute(cli: CommandLineInterface) = {
    cli.subcommand match {
      case Some(cli.execute) =>
        if (cli.execute.importOnly()) R2BCli.doImport(getConfiguration(cli))
        else R2BCli.migrate(getConfiguration(cli))
      case Some(cli.init) =>
        R2BCli.init(getConfiguration(cli))
      case _ =>
        R2BCli.help()
    }
  }

  private[this] def getConfiguration(cli: CommandLineInterface) = {
    val keys: Array[String] = cli.execute.projectKey().split(":")
    val redmine: String     = keys(0)
    val backlog: String     = if (keys.length == 2) keys(1) else keys(0).toUpperCase.replaceAll("-", "_")

    ConsoleOut.info(s"""--------------------------------------------------
     |${Messages("common.redmine")} ${Messages("common.url")}[${cli.execute.redmineUrl()}]
     |${Messages("common.redmine")} ${Messages("common.access_key")}[${cli.execute.redmineKey()}]
     |${Messages("common.redmine")} ${Messages("common.project_key")}[${redmine}]
     |${Messages("common.backlog")} ${Messages("common.url")}[${cli.execute.backlogUrl()}]
     |${Messages("common.backlog")} ${Messages("common.access_key")}[${cli.execute.backlogKey()}]
     |${Messages("common.backlog")} ${Messages("common.project_key")}[${backlog}]
     |${Messages("common.importOnly")}[${cli.execute.importOnly()}]
     |${Messages("common.optOut")}[${cli.execute.optOut.toOption.getOrElse(false)}]
     |--------------------------------------------------
     |""".stripMargin)

    AppConfiguration(
      redmineConfig = new RedmineApiConfiguration(url = cli.execute.redmineUrl(), key = cli.execute.redmineKey(), projectKey = redmine),
      backlogConfig = new BacklogApiConfiguration(url = cli.execute.backlogUrl(), key = cli.execute.backlogKey(), projectKey = backlog),
      importOnly = cli.execute.importOnly(),
      optOut = cli.execute.optOut())
  }

  private[this] def setLang() = {
    if (language == "ja") {
      Locale.setDefault(Locale.JAPAN)
    } else if (language == "en") {
      Locale.setDefault(Locale.US)
    }
  }

  private[this] def checkRelease() = {
    try {
      val string = scala.io.Source.fromURL("https://api.github.com/repos/nulab/BacklogMigration-Redmine/releases").mkString
      val latest = string.parseJson match {
        case JsArray(releases) if (releases.nonEmpty) =>
          releases(0).asJsObject.fields.apply("tag_name").toString().replace("\"", "").replace("v", "")
        case _ => ""
      }
      if (latest != versionName) {
        ConsoleOut.warning(s"""
            |--------------------------------------------------
            |${Messages("cli.warn.not.latest", latest, versionName)}
            |--------------------------------------------------
          """.stripMargin)
      }
    } catch {
      case e: Throwable => logger.error(e.getMessage, e)
    }
  }

}