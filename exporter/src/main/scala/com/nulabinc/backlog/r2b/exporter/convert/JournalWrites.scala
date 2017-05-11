package com.nulabinc.backlog.r2b.exporter.convert

import javax.inject.Inject

import com.nulabinc.backlog.migration.common.convert.{Convert, Writes}
import com.nulabinc.backlog.migration.common.domain.{BacklogComment, BacklogNotification}
import com.nulabinc.backlog.migration.common.utils.{DateUtil, StringUtil}
import com.taskadapter.redmineapi.bean.Journal

import scala.collection.JavaConverters._

/**
  * @author uchida
  */
private[exporter] class JournalWrites @Inject()(implicit val userWrites: UserWrites, implicit val journalDetailWrites: JournalDetailWrites)
    extends Writes[Journal, BacklogComment] {

  override def writes(journal: Journal): BacklogComment = {
    BacklogComment(
      eventType = "comment",
      optIssueId = None,
      optContent = StringUtil.notEmpty(journal.getNotes),
      changeLogs = journal.getDetails.asScala.map(Convert.toBacklog(_)),
      notifications = Seq.empty[BacklogNotification],
      isCreateIssue = false,
      optCreatedUser = Option(journal.getUser).map(Convert.toBacklog(_)),
      optCreated = Option(journal.getCreatedOn).map(DateUtil.isoFormat)
    )
  }

}
