package uk.vitalcode.events.api.models

case class Page[T](total: Int,
                   items: Seq[T])
