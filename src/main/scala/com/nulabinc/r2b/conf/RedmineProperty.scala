package com.nulabinc.r2b.conf

/**
  * @author uchida
  */
object RedmineProperty {

  object FieldFormat {
    val VERSION: String = "version"
    val USER: String = "user"
    val STRING: String = "string"
    val LINK: String = "link"
    val INT: String = "int"
    val FLOAT: String = "float"
    val DATE: String = "date"
    val TEXT: String = "text"
    val LIST: String = "list"
    val BOOL: String = "bool"
  }

  val ATTACHMENT: String = "attachment"
  val CUSTOM_FIELD: String = "cf"
  val ATTR: String = "attr"
  val RELATION: String = "relation"

  object Attr {
    val SUBJECT: String = "subject"
    val DESCRIPTION: String = "description"
    val TRACKER: String = "tracker_id"
    val STATUS: String = "status_id"
    val PRIORITY: String = "priority_id"
    val ASSIGNED: String = "assigned_to_id"
    val VERSION: String = "fixed_version_id"
    val PARENT: String = "parent_id"
    val START_DATE: String = "start_date"
    val DUE_DATE: String = "due_date"
    val ESTIMATED_HOURS: String = "estimated_hours"
    val CATEGORY: String = "category_id"
  }

}
