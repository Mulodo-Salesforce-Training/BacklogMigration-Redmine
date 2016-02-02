package com.nulabinc.r2b.actor.redmine

import java.util.UUID._

import akka.actor.SupervisorStrategy.Escalate
import akka.actor._
import com.nulabinc.r2b.actor.utils.{R2BLogging, Subtasks}
import com.nulabinc.r2b.conf.R2BConfig
import com.nulabinc.r2b.service.RedmineService
import com.osinka.i18n.Messages
import com.taskadapter.redmineapi.bean.{Project, WikiPage}

/**
  * @author uchida
  */
class WikisActor(conf: R2BConfig, project: Project) extends Actor with R2BLogging with Subtasks {

  override val supervisorStrategy = OneForOneStrategy(maxNrOfRetries = 0) {
    case _: Exception =>
      Escalate
  }

  var wikiSize: Int = 0

  def receive: Receive = {
    case WikisActor.Do =>

      val redmineService: RedmineService = new RedmineService(conf)
      val wikiPages = redmineService.getWikiPagesByProject(project.getIdentifier)

      if (wikiPages.nonEmpty) {

        wikiSize = wikiPages.size
        info(Messages("message.execute_redmine_wikis_export", project.getName, wikiSize))
        wikiPages.foreach(contents)

      } else context.stop(self)

    case Terminated(ref) =>
      info(Messages("message.execute_redmine_wiki_export", project.getName, wikiSize - subtasks.size + 1, wikiSize))
      complete(ref)
      if (subtasks.isEmpty) context.stop(self)
  }

  private def contents(wikiPage: WikiPage) = {
    val wikiActor = start(Props(new WikiActor(conf, project, wikiPage.getTitle)), WikiActor.actorName)
    wikiActor ! WikiActor.Do
  }

}

object WikisActor {

  case class Do()

  def actorName = s"WikisActor_$randomUUID"

}
