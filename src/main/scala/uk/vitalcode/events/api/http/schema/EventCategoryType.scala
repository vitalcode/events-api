package uk.vitalcode.events.api.http.schema

import sangria.schema.{EnumType, EnumValue}
import uk.vitalcode.events.model.Category
import uk.vitalcode.events.model.Category._

trait EventCategoryType {

  val EventCategoryType = EnumType[Category](
    "EventCategory",
    Some("Event category"),
    Category.values.toList.map(category =>
      EnumValue(category.toString,
        value = category,
        description = Some(s"${category.toString} event category")))
  )
}
