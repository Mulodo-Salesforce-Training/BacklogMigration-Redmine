package com.nulabinc.r2b.cli

import com.nulabinc.backlog.importer.core.BacklogConfig
import com.nulabinc.backlog4j.{User => BacklogUser}
import com.nulabinc.r2b.actor.prepare.FindUsersActor
import com.nulabinc.r2b.conf.{ConfigBase, R2BConfig}
import com.nulabinc.r2b.domain._
import com.nulabinc.r2b.service.{BacklogService, RedmineService}
import com.osinka.i18n.Messages
import com.taskadapter.redmineapi.bean.{User => RedmineUser}

/**
 * @author uchida
 */
class UserMapping(r2bConf: R2BConfig) extends MappingManager {

  private val backlogDatas = loadBacklog()
  private val redmineDatas = loadRedmine()

  private def loadRedmine(): Seq[MappingItem] = {
    val redmineService: RedmineService = new RedmineService(r2bConf)

    printlog("- " + Messages("mapping.load_redmine", itemName))
    printlog("-  " + Messages("message.collect_project_user"))
    
    val redmineUsers: Seq[RedmineUser] = FindUsersActor(r2bConf).toSeq.map(user => {
      if (Option(user.getLogin).isDefined && Option(user.getFullName).isDefined) user
      else redmineService.getUserById(user.getId)
    })

    val redmines: Seq[MappingItem] = redmineUsers.map(redmineUser => MappingItem(redmineUser.getLogin, redmineUser.getFullName))
    redmines
  }

  private def loadBacklog(): Seq[MappingItem] = {
    printlog("- " + Messages("mapping.load_backlog", itemName))
    val backlogService: BacklogService = new BacklogService(BacklogConfig(r2bConf.backlogUrl, r2bConf.backlogKey))
    val backlogUsers: Seq[BacklogUser] = backlogService.getUsers
    val backlogs: Seq[MappingItem] = backlogUsers.map(backlogUser => MappingItem(backlogUser.getUserId, backlogUser.getName))
    backlogs
  }

  override def matchWithBacklog(redmine: MappingItem): String =
    backlogs.map(_.name).find(_ == redmine.name).getOrElse("")

  override def backlogs: Seq[MappingItem] = backlogDatas

  override def redmines: Seq[MappingItem] = redmineDatas

  override def filePath: String = ConfigBase.USER_MAPPING_FILE

  override def itemName: String = Messages("users")

  override def description: String = {
    val description: String =
      Messages("mapping.possible_values", itemName, backlogs.map(_.name).mkString(","))
    description
  }

  override def isDisplayDetail: Boolean = true

}
