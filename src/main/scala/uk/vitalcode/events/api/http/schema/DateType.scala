package uk.vitalcode.events.api.http.schema

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import sangria.ast
import sangria.schema.ScalarType
import sangria.validation.ValueCoercionViolation

import scala.util.{Failure, Success, Try}

trait DateType {

  case object DateCoercionViolation extends ValueCoercionViolation("Date value expected")

  private def parseDate(s: String) = Try(LocalDateTime.parse(s, DateTimeFormatter.ISO_LOCAL_DATE_TIME)) match {
    case Success(date) => Right(date)
    case Failure(_) => Left(DateCoercionViolation)
  }

  val DateType = ScalarType[LocalDateTime]("Date",
    description = Some("An example of date scalar type"),
    coerceOutput = (d, _) ⇒ d.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
    coerceUserInput = {
      case s: String ⇒ parseDate(s)
      case _ ⇒ Left(DateCoercionViolation)
    },
    coerceInput = {
      case ast.StringValue(s, _, _) ⇒ parseDate(s)
      case _ ⇒ Left(DateCoercionViolation)
    })
}
