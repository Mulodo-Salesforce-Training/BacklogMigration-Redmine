package com.nulabinc.backlog.r2b.exporter.core

import javax.inject.Inject

import com.nulabinc.backlog.migration.common.conf.BacklogPaths
import com.nulabinc.backlog.r2b.exporter.convert._
import com.nulabinc.backlog.r2b.mapping.service.{MappingPriorityService, MappingUserService}
import com.nulabinc.backlog.r2b.redmine.conf.RedmineApiConfiguration
import com.nulabinc.backlog.r2b.redmine.domain.PropertyValue
import com.nulabinc.backlog.r2b.redmine.service.{IssueService, ProjectService}

class ExportContextProvider @Inject()(projectWrites: ProjectWrites,
                                      customFieldDefinitionsWrites: CustomFieldDefinitionsWrites,
                                      versionsWrites: VersionsWrites,
                                      issueTypesWrites: IssueTypesWrites,
                                      issueCategoriesWrites: IssueCategoriesWrites,
                                      newsWrites: NewsWrites,
                                      membershipWrites: MembershipWrites,
                                      apiConfig: RedmineApiConfiguration,
                                      backlogPaths: BacklogPaths,
                                      issueService: IssueService,
                                      projectService: ProjectService,
                                      propertyValue: PropertyValue,
                                      issueWrites: IssueWrites,
                                      journalWrites: JournalWrites,
                                      userWrites: UserWrites,
                                      customFieldWrites: CustomFieldWrites,
                                      customFieldValueWrites: CustomFieldValueWrites,
                                      attachmentWrites: AttachmentWrites,
                                      mappingUserService: MappingUserService,
                                      mappingPriorityService: MappingPriorityService) {

  def get(): ExportContext = {
    ExportContext(mappingUserService,
                  mappingPriorityService,
                  apiConfig,
                  backlogPaths,
                  propertyValue,
                  issueWrites,
                  journalWrites,
                  userWrites,
                  customFieldWrites,
                  customFieldValueWrites,
                  attachmentWrites)
  }

}
