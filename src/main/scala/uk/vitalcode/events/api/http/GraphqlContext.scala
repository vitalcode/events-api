package uk.vitalcode.events.api.http

import uk.vitalcode.events.api.models.UserEntity
import uk.vitalcode.events.api.utils.JwtUtils


class GraphqlContext(var subject: Option[UserEntity] = None) {

  def setSubject(user: Option[UserEntity]): Unit = subject = user

  def setSubject(token: String): Unit = setSubject(JwtUtils.decode(token))
}
