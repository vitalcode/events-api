package me.archdev.restapi.models

import java.time.LocalDateTime

import uk.vitalcode.events.model.Category._

case class Event(id: String,
                 category: Option[Seq[Category]],
                 description: Option[Seq[String]],
                 from: Option[Seq[LocalDateTime]])
