package me.archdev.restapi.models

case class Page[T](total: Int,
                   items: Seq[T])
