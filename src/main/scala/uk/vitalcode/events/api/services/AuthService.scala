package uk.vitalcode.events.api.services

import uk.vitalcode.events.api.models.db.TokenEntityTable
import uk.vitalcode.events.api.models.{AuthenticationException, UserEntity}
import uk.vitalcode.events.api.utils.{DatabaseService, JwtUtils}

import scala.concurrent.{ExecutionContext, Future}

class AuthService(val databaseService: DatabaseService, usersService: UsersService)(implicit executionContext: ExecutionContext) extends TokenEntityTable {

  import databaseService._
  import databaseService.driver.api._

  // TODO password hash
  def login(username: String, password: String): Future[String] = {
    val result: Future[Seq[UserEntity]] = db.run(users.filter(u => u.username === username && u.password === password).result)
    result.map {
      case Seq(user) => JwtUtils.encode(user)
      case _ => throw AuthenticationException("UserName or password is incorrect")
    }
  }

  def login(user: UserEntity): Future[String] = login(user.username, user.password)

  //def logout(token: TokenEntity) = db.run(tokens.filter(_.id === token.id).delete)

  def signup(newUser: UserEntity): Future[String] = {
    usersService.createUser(newUser).map(user => createToken(user))
  }

  def signup(login: String, pass: String): Future[String] = {
    val newUser = UserEntity(
      username = login,
      password = pass,
      permissions = None
    )
    signup(newUser)
  }

  def authorise(token: String): Future[Option[UserEntity]] = db.run((for {
    token <- tokens.filter(_.token === token)
    user <- users.filter(_.id === token.userId)
  } yield user).result.headOption)

  def createToken(user: UserEntity): String = {
    JwtUtils.encode(user)
  }

//  def getToken(token: String): Future[Option[TokenEntity]] = db.run(tokens.filter(_.token === token).result.headOption)
//
//  def tokenByUser(user: UserEntity): Future[Option[TokenEntity]] = db.run(tokens.filter(_.userId === user.id).result.headOption)
}
