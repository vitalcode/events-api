package me.archdev.restapi.services

import me.archdev.restapi.models.db.TokenEntityTable
import me.archdev.restapi.models.{TokenEntity, UserEntity}
import me.archdev.restapi.utils.DatabaseService

import scala.concurrent.{ExecutionContext, Future}

trait AuthService extends TokenEntityTable {
  this: UsersService =>

  implicit val executionContext: ExecutionContext
  val databaseService: DatabaseService

  import databaseService._
  import databaseService.driver.api._

  def signIn(login: String, password: String): Future[Option[TokenEntity]] = {
    // TODO password hash
    db.run(users.filter(u => u.username === login).result)
      .flatMap { users => users.find(user => user.password == password) match {
        case Some(user) => db.run(tokens.filter(_.userId === user.id).result.headOption).flatMap {
          case Some(token) => Future.successful(Some(token))
          case None => createToken(user).map(token => Some(token))
        }
        case None => Future.successful(None)
      }
      }
  }

  def signUp(newUser: UserEntity): Future[TokenEntity] = {
    createUser(newUser).flatMap(user => createToken(user))
  }

  def authorise(token: String): Future[Option[UserEntity]] = db.run((for {
    token <- tokens.filter(_.token === token)
    user <- users.filter(_.id === token.userId)
  } yield user).result.headOption)

  def createToken(user: UserEntity): Future[TokenEntity] = db.run(tokens returning tokens += TokenEntity(userId = user.id))
}

