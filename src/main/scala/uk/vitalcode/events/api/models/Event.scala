package uk.vitalcode.events.api.models

import java.time.LocalDateTime

import uk.vitalcode.events.model.Category._

case class Event(id: String,
                 url: Option[Seq[String]],
                 title: Option[Seq[String]],
                 from: Option[Seq[LocalDateTime]],
                 to: Option[Seq[LocalDateTime]],
                 category: Option[Seq[Category]],
                 description: Option[Seq[String]],
                 image: Option[Seq[String]],
                 cost: Option[Seq[String]],
                 telephone: Option[Seq[String]],
                 venue: Option[Seq[String]],
                 venueCategory: Option[Seq[String]]
                )
