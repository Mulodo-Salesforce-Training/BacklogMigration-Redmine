package com.nulabinc.r2b.cli

import com.nulabinc.backlog.importer.core.BacklogConfig
import com.nulabinc.backlog4j.Priority
import com.nulabinc.r2b.conf.{ConfigBase, R2BConfig}
import com.nulabinc.r2b.domain.MappingItem
import com.nulabinc.r2b.service.{BacklogService, RedmineService}
import com.osinka.i18n.Messages

/**
 * @author uchida
 */
class PriorityMapping(conf: R2BConfig) extends MappingManager {

  private val backlogDatas = loadBacklog()
  private val redmineDatas = loadRedmine()

  private def loadRedmine(): Seq[MappingItem] = {
    info("- " + Messages("mapping.load_redmine", itemName))
    val redmineService: RedmineService = new RedmineService(conf)
    val redminePriorities = redmineService.getIssuePriorities()
    val redmines: Seq[MappingItem] = redminePriorities.map(redminePriority => MappingItem(redminePriority.getName, redminePriority.getName))
    redmines
  }

  private def loadBacklog(): Seq[MappingItem] = {
    info("- " + Messages("mapping.load_backlog", itemName))
    val backlogService: BacklogService = new BacklogService(BacklogConfig(conf.backlogUrl, conf.backlogKey))
    val backlogPriorities: Seq[Priority] = backlogService.getPriorities
    val backlogs: Seq[MappingItem] = backlogPriorities.map(backlogPriority => MappingItem(backlogPriority.getName, backlogPriority.getName))
    backlogs
  }

  private object Backlog {
    val LOW_JA: String = "低"
    val NORMAL_JA: String = "中"
    val HIGH_JA: String = "高"
    val LOW_EN: String = "Low"
    val NORMAL_EN: String = "Normal"
    val HIGH_EN: String = "High"

    def low(): String = backlogs.map(_.name).find(_ == LOW_JA).getOrElse(backlogs.map(_.name).find(_ == LOW_EN).getOrElse(""))

    def normal(): String = backlogs.map(_.name).find(_ == NORMAL_JA).getOrElse(backlogs.map(_.name).find(_ == NORMAL_EN).getOrElse(""))

    def high(): String = backlogs.map(_.name).find(_ == HIGH_JA).getOrElse(backlogs.map(_.name).find(_ == HIGH_EN).getOrElse(""))
  }

  private object Redmine {
    val LOW_JA: String = "低め"
    val NORMAL_JA: String = "通常"
    val HIGH_JA: String = "高め"
    val URGENT_JA: String = "急いで"
    val IMMEDIATE_JA: String = "今すぐ"
    val LOW_EN: String = "Low"
    val NORMAL_EN: String = "Normal"
    val HIGH_EN: String = "High"
    val URGENT_EN: String = "Urgent"
    val IMMEDIATE_EN: String = "Immediate"
  }

  override def matchWithBacklog(redmine: MappingItem): String = {
    val option: Option[String] = backlogs.map(_.name).find(_ == redmine)
    option match {
      case Some(backlog) => backlog
      case None => redmine.name match {
        case Redmine.LOW_JA | Redmine.LOW_EN => Backlog.low()
        case Redmine.NORMAL_JA | Redmine.NORMAL_EN => Backlog.normal()
        case Redmine.HIGH_JA | Redmine.HIGH_EN => Backlog.high()
        case Redmine.URGENT_JA | Redmine.URGENT_EN => ""
        case Redmine.IMMEDIATE_JA | Redmine.IMMEDIATE_EN => ""
        case _ => ""
      }
    }
  }

  override def backlogs: Seq[MappingItem] = backlogDatas

  override def redmines: Seq[MappingItem] = redmineDatas

  override def filePath: String = ConfigBase.PRIORITY_MAPPING_FILE

  override def itemName: String = Messages("priorities")

  override def description: String =
    Messages("mapping.possible_values", itemName, backlogs.map(_.name).mkString(","))

  override def isDisplayDetail: Boolean = false

}