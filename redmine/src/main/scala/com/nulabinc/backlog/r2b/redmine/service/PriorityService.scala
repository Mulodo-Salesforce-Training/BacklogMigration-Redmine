package com.nulabinc.backlog.r2b.redmine.service

import com.taskadapter.redmineapi.bean.IssuePriority

/**
  * @author uchida
  */
trait PriorityService {

  def allPriorities(): Seq[IssuePriority]

}
