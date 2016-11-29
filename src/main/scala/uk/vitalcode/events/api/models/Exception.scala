package uk.vitalcode.events.api.models

case class AuthenticationException(message: String) extends Exception(message)

case class AuthorisationException(message: String) extends Exception(message)