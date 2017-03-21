package com.nulabinc.r2b.cli

import java.util.Locale

import com.nulabinc.r2b.mapping.{Mapping, MappingItem}
import com.osinka.i18n.{Lang, Messages}

/**
  * @author uchida
  */
class MappingValidator(redmineMappings: Seq[MappingItem], backlogMappings: Seq[MappingItem], itemName: String, filePath: String) {

  implicit val userLang = if (Locale.getDefault.equals(Locale.JAPAN)) Lang("ja") else Lang("en")

  def validate(optMappings: Option[Seq[Mapping]]): Seq[String] =
    optMappings match {
      case Some(mappings) =>
        itemsExists(mappings) union
          itemsRequired(mappings) union
          redmineItemsExists(mappings)
      case _ => throw new RuntimeException
    }

  private[this] def redmineItemsExists(mappings: Seq[Mapping]): Seq[String] =
    redmineMappings.foldLeft(Seq.empty[String])((errors: Seq[String], mappingItem: MappingItem) =>
      redmineItemExists(mappingItem, mappings) match {
        case Some(error) => errors :+ error
        case None => errors
      })

  private[this] def redmineItemExists(mappingItem: MappingItem, mappings: Seq[Mapping]): Option[String] =
    if (!mappings.exists(mapping => mapping.redmine == mappingItem.name))
      Some("- " + Messages("mapping.not_exists_redmine_item", itemName, mappingItem.name, filePath))
    else None

  private[this] def itemsExists(mappings: Seq[Mapping]): Seq[String] =
    mappings.foldLeft(Seq.empty[String])((errors: Seq[String], mapping: Mapping) => {
      itemExists(mapping) match {
        case Some(error) => errors :+ error
        case None => errors
      }
    })

  private[this] def itemExists(mapping: Mapping): Option[String] =
    if (mapping.backlog.nonEmpty && !backlogMappings.exists(_.name == mapping.backlog)) {
      Some("- " + Messages("mapping.not_exist_backlog", mapping.backlog, itemName))
    } else if (mapping.redmine.nonEmpty && !redmineMappings.exists(_.name == mapping.redmine)) {
      Some("- " + Messages("mapping.not_exist_redmine", mapping.redmine, itemName))
    } else None

  private[this] def itemsRequired(mappings: Seq[Mapping]): Seq[String] =
    mappings.foldLeft(Seq.empty[String])((errors: Seq[String], mapping: Mapping) => {
      itemRequired(mapping) match {
        case Some(error) => errors :+ error
        case None => errors
      }
    })

  private[this] def itemRequired(mapping: Mapping): Option[String] =
    if (mapping.backlog.isEmpty) Some("- " + Messages("mapping.specify_item", itemName, mapping.redmine))
    else None

}
