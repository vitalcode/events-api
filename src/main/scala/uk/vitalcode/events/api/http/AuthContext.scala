package uk.vitalcode.events.api.http

import uk.vitalcode.events.api.models.UserEntity
import uk.vitalcode.events.api.utils.JwtUtils


case class AuthContext(subject: Option[UserEntity] = None) {
  def withToken(token: String): AuthContext = this.copy(subject = JwtUtils.decode(token))
}
