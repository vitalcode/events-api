package uk.vitalcode.events.api.utils

import pdi.jwt.{Jwt, JwtAlgorithm, JwtClaim, JwtHeader}
import spray.json._
import DefaultJsonProtocol._
import uk.vitalcode.events.api.models.UserEntity

import scala.util.Try

object JwtUtils {

  val expiration = 2 * 60
  val algorithm = JwtAlgorithm.HS256
  val secret = "lisenok"

  implicit val userEntityFormat = jsonFormat4(UserEntity.apply)

  def encode(user: UserEntity): String = {
    val claim = JwtClaim(user.toJson.compactPrint)
      .issuedNow
      .expiresIn(expiration)

    Jwt.encode(claim, secret, algorithm)
  }

  def decode(token: String): Try[UserEntity] = {
    Jwt.decodeRaw(token, secret, Seq(algorithm)).map(_.parseJson.convertTo[UserEntity])
  }

  def isValid(token: String): Boolean = {
    Jwt.isValid(token, secret, Seq(JwtAlgorithm.HS256))
  }
}

