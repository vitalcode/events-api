package uk.vitalcode.events.api.test.utils

import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.model.MediaTypes._
import akka.http.scaladsl.unmarshalling.Unmarshaller
import spray.json.{JsArray, JsObject, JsString, _}


trait GraphqlErrorSupport {

  case class GraphqlError(message: String, path: String)

  implicit val GraphqlErrorUnmarshaller: Unmarshaller[HttpEntity, GraphqlError] = {
    Unmarshaller.byteStringUnmarshaller
      .forContentTypes(`application/json`)
      .mapWithCharset {
        (data, charset) => {
          val json = data.utf8String.parseJson.asJsObject
          val errors = json.getFields("errors").head.asInstanceOf[JsArray].elements.head.asInstanceOf[JsObject]
          val message = errors.fields("message").asInstanceOf[JsString].value
          val path = errors.fields("path").asInstanceOf[JsArray].elements.head.asInstanceOf[JsString].value
          GraphqlError(message, path)
        }
      }
  }
}