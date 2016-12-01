package uk.vitalcode.events.api.test

import org.scalatest._
import pdi.jwt.Jwt
import uk.vitalcode.events.api.models.UserEntity
import uk.vitalcode.events.api.utils.JwtUtils

class JwtUtilsTest extends WordSpec with Matchers {
  "JwtUtils" should {
    "produce JWT token for provided user" in {
      val user = UserEntity(Some(1), "username", "password", Some("admin"))
      val token = JwtUtils.encode(user)

      val ff = Jwt.decodeRaw(token, JwtUtils.secret, Seq(JwtUtils.algorithm))
      1 shouldBe 1
    }
  }
}