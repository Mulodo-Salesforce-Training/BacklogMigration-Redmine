package com.nulabinc.r2b.service

import java.io.{File, FileOutputStream}
import java.net.URL
import java.nio.channels.{Channels, ReadableByteChannel}

import com.nulabinc.r2b.cli.ParamProjectKey
import com.nulabinc.r2b.conf.R2BConfig
import com.nulabinc.r2b.domain.{RedmineJsonProtocol, RedmineIssuesWrapper}
import com.taskadapter.redmineapi._
import com.taskadapter.redmineapi.bean._
import spray.json.JsonParser

import scala.collection.JavaConversions._
import scala.io.Source

/**
 * @author uchida
 */
class RedmineService(r2bConf: R2BConfig) {
  import RedmineJsonProtocol._

  val redmine: RedmineManager = RedmineManagerFactory.createWithApiKey(r2bConf.redmineUrl, r2bConf.redmineKey)

  def getIssuesCount(projectId: Int): Int = {
    val source = Source.fromURL(r2bConf.redmineUrl + "/issues.json?limit=1&project_id=" + projectId)
    val redmineIssuesWrapper: RedmineIssuesWrapper = JsonParser(source.getLines.mkString).convertTo[RedmineIssuesWrapper]
    redmineIssuesWrapper.total_count
  }

  def getIssues(params: Map[String, String]): Seq[Issue] = {
    redmine.getIssueManager.getIssues(params)
  }

  def getIssueById(id: Integer, include: Include*): Issue = {
    redmine.getIssueManager.getIssueById(id, include: _*)
  }

  def getCustomFieldDefinitions: Either[Throwable, Seq[CustomFieldDefinition]] = {
    try {
      Right(redmine.getCustomFieldManager.getCustomFieldDefinitions)
    } catch {
      case e: NotFoundException => Left(e)
    }
  }

  def getProjects: Seq[Project] = {
    val projects: Seq[Either[Throwable, Project]] = r2bConf.projects.map(getProject)
    projects.filter(_.isRight).map(_.right.get)
  }

  def getProject(projectKey: ParamProjectKey): Either[Throwable, Project] =
    try {
      Right(redmine.getProjectManager.getProjectByKey(projectKey.redmine))
    } catch {
      case e: NotFoundException => Left(e)
    }

  def getMemberships(projectKey: String): Either[Throwable, Seq[Membership]] = {
    try {
      Right(redmine.getMembershipManager.getMemberships(projectKey))
    } catch {
      case e: NotFoundException => Left(e)
    }
  }

  def getCategories(projectId: Int): Either[Throwable, Seq[IssueCategory]] = {
    try {
      Right(redmine.getIssueManager.getCategories(projectId))
    } catch {
      case e: NotFoundException => Left(e)
    }
  }

  def getTrackers: Either[Throwable, Seq[Tracker]] = {
    try {
      Right(redmine.getIssueManager.getTrackers)
    } catch {
      case e: NotFoundException => Left(e)
    }
  }

  def getWikiPagesByProject(projectKey: String): Either[Throwable, Seq[WikiPage]] = {
    try {
      Right(redmine.getWikiManager.getWikiPagesByProject(projectKey))
    } catch {
      case e: RedmineAuthenticationException => Left(e)
    }
  }

  def getWikiPageDetailByProjectAndTitle(projectKey: String, pageTitle: String): WikiPageDetail = {
    redmine.getWikiManager.getWikiPageDetailByProjectAndTitle(projectKey, pageTitle)
  }

  def getUsers: Seq[User] = {
    val users: Seq[User] = redmine.getUserManager.getUsers
    users.map(user => getUserById(user.getId))
  }

  def getUserById(id: Int): User = {
    redmine.getUserManager.getUserById(id)
  }

  def getNews(projectKey: String): Seq[News] = {
    redmine.getProjectManager.getNews(projectKey)
  }

  def getGroups: Either[Throwable, Seq[Group]] = {
    try {
      Right(redmine.getUserManager.getGroups)
    } catch {
      case e: RedmineAuthenticationException => Left(e)
    }
  }

  def getGroupById(id: Int): Group = {
    redmine.getUserManager.getGroupById(id)
  }

  def getStatuses: Either[Throwable, Seq[IssueStatus]] = {
    try {
      Right(redmine.getIssueManager.getStatuses)
    } catch {
      case e: NotFoundException => Left(e)
    }
  }

  def getIssuePriorities: Either[Throwable, Seq[IssuePriority]] = {
    try {
      Right(redmine.getIssueManager.getIssuePriorities)
    } catch {
      case e: NotFoundException => Left(e)
    }
  }

  def downloadAttachmentContent(attachment: Attachment, path: String) = {
    val website: URL = new URL(attachment.getContentURL + "?key=" + r2bConf.redmineKey)
    val rbc: ReadableByteChannel = Channels.newChannel(website.openStream())
    val fos: FileOutputStream = new FileOutputStream(path + File.separator + attachment.getFileName)
    fos.getChannel.transferFrom(rbc, 0, java.lang.Long.MAX_VALUE)
  }

  def getVersions(projectID: Int): Either[Throwable, Seq[Version]] = {
    try {
      Right(redmine.getProjectManager.getVersions(projectID))
    } catch {
      case e: RedmineAuthenticationException => Left(e)
    }
  }

  def getMemberships(projectID: Int): Either[Throwable, Seq[Membership]] = {
    try {
      Right(redmine.getMembershipManager.getMemberships(projectID))
    } catch {
      case e: NotFoundException => Left(e)
    }
  }

}