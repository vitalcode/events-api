package uk.vitalcode.events.api.services

import uk.vitalcode.events.api.models.db.TokenEntityTable
import uk.vitalcode.events.api.models.{TokenEntity, UserEntity}
import uk.vitalcode.events.api.utils.DatabaseService

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

trait AuthService extends TokenEntityTable {
  this: UsersService =>

  implicit val executionContext: ExecutionContext
  val databaseService: DatabaseService

  import databaseService._
  import databaseService.driver.api._

  // TODO password hash
  def login(username: String, password: String): Future[Option[TokenEntity]] = {
    db.run(users.filter(u => u.username === username).result)
      .flatMap { users => users.find(user => user.password == password) match {
        case Some(user) => db.run(tokens.filter(_.userId === user.id).result.headOption).flatMap {
          case Some(token) => Future.successful(Some(token))
          case None => createToken(user).map(token => Some(token))
        }
        case None => Future.successful(None)
      }
      }
  }




  def signup(newUser: UserEntity): Future[TokenEntity] = {
    createUser(newUser).flatMap(user => createToken(user))
  }

  def signup(login: String, pass: String): Future[TokenEntity] = {
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

  def createToken(user: UserEntity): Future[TokenEntity] = {
    val userTokenQuery = tokens.filter(_.userId === user.id)

    val t: Seq[TokenEntity] = Await.result(db.run(userTokenQuery.result), Duration.Inf)

    if (t.isEmpty) {
      db.run(tokens returning tokens += TokenEntity(userId = user.id))
    }
    else Future(t.head)
  }

  def getToken(token: String): Future[Option[TokenEntity]] = db.run(tokens.filter(_.token === token).result.headOption)
}
