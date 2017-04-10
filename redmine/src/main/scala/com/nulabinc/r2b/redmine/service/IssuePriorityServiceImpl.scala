package com.nulabinc.r2b.redmine.service

import javax.inject.Inject

import com.nulabinc.backlog.migration.utils.Logging
import com.taskadapter.redmineapi.RedmineManager
import com.taskadapter.redmineapi.bean.IssuePriority

import scala.collection.JavaConverters._

/**
  * @author uchida
  */
class IssuePriorityServiceImpl @Inject()(redmine: RedmineManager) extends IssuePriorityService with Logging {

  override def allIssuePriorities(): Seq[IssuePriority] = {
    try {
      redmine.getIssueManager.getIssuePriorities.asScala
    } catch {
      case e: Throwable =>
        logger.error(e.getMessage, e)
        Seq.empty[IssuePriority]
    }
  }

}
