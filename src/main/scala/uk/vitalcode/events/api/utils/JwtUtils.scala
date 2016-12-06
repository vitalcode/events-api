package uk.vitalcode.events.api.utils

import pdi.jwt.{Jwt, JwtAlgorithm, JwtClaim}
import spray.json.DefaultJsonProtocol._
import spray.json._
import uk.vitalcode.events.api.models.UserEntity

object JwtUtils extends Config {

  val algorithm = JwtAlgorithm.HS256

  implicit val userEntityFormat = jsonFormat4(UserEntity.apply)

  def createToken(subject: UserEntity): String = {
    val claim = JwtClaim(subject.toJson.compactPrint)
      .issuedNow
      .expiresIn(jwtExpiration)

    Jwt.encode(claim, jwtSecret, algorithm)
  }

  def decodeSubject(token: String): Option[UserEntity] = {
    Jwt.decodeRaw(token, jwtSecret, Seq(algorithm)).map(_.parseJson.convertTo[UserEntity]).toOption
  }

  def isTokenValid(token: String): Boolean = {
    Jwt.isValid(token, jwtSecret, Seq(JwtAlgorithm.HS256))
  }
}

