package com.nulabinc.backlog.r2b.exporter.actor

import java.util.concurrent.CountDownLatch
import javax.inject.Inject

import akka.actor.SupervisorStrategy.Restart
import akka.actor.{Actor, OneForOneStrategy, Props}
import akka.routing.SmallestMailboxPool
import com.nulabinc.backlog.migration.common.conf.{BacklogConfiguration, BacklogPaths}
import com.nulabinc.backlog.migration.common.modules.akkaguice.NamedActor
import com.nulabinc.backlog.migration.common.utils.{Logging, ProgressBar}
import com.nulabinc.backlog.r2b.exporter.convert.WikiWrites
import com.nulabinc.backlog.r2b.exporter.core.ExportContext
import com.nulabinc.backlog.r2b.redmine.conf.RedmineApiConfiguration
import com.nulabinc.backlog.r2b.redmine.service.WikiService
import com.osinka.i18n.Messages
import com.taskadapter.redmineapi.bean.WikiPage

import scala.concurrent.duration._

/**
  * @author uchida
  */
private[exporter] class WikisActor @Inject()(apiConfig: RedmineApiConfiguration,
                                             backlogPaths: BacklogPaths,
                                             wikiWrites: WikiWrites,
                                             wikiService: WikiService)
    extends Actor
    with BacklogConfiguration
    with Logging {

  private[this] val strategy = OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 minute) {
    case _ => Restart
  }

  private[this] val wikis: Seq[WikiPage] = wikiService.allWikis()
  private[this] val completion           = new CountDownLatch(wikis.size)
  private[this] val console              = (ProgressBar.progress _)(Messages("common.wikis"), Messages("message.exporting"), Messages("message.exported"))

  def receive: Receive = {
    case WikisActor.Do(exportContext) =>
      val router    = SmallestMailboxPool(akkaMailBoxPool, supervisorStrategy = strategy)
      val wikiActor = context.actorOf(router.props(Props(new WikiActor(apiConfig, backlogPaths, wikiWrites, wikiService))))

      wikis.foreach(wiki => wikiActor ! WikiActor.Do(wiki, completion, wikis.size, console))

      completion.await
      sender() ! WikisActor.Done(exportContext)
  }

}

private[exporter] object WikisActor extends NamedActor {

  override final val name = "WikisActor"

  case class Do(exportContext: ExportContext)

  case class Done(exportContext: ExportContext)

}
