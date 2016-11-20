package uk.vitalcode.events.api.services

import uk.vitalcode.events.api.models.db.UserEntityTable
import uk.vitalcode.events.api.models.{UserEntity, UserEntityUpdate}
import uk.vitalcode.events.api.utils.DatabaseService

import scala.concurrent.{ExecutionContext, Future}

trait UsersService extends UserEntityTable {

  implicit val executionContext: ExecutionContext
  val databaseService: DatabaseService

  import databaseService._
  import databaseService.driver.api._

  def getUsers: Future[Seq[UserEntity]] = db.run(users.sortBy(u => u.id).result)

  def getUserById(id: Long): Future[Option[UserEntity]] = db.run(users.filter(_.id === id).result.headOption)

  def getUserByLogin(login: String): Future[Option[UserEntity]] = db.run(users.filter(_.username === login).result.headOption)

  def createUser(user: UserEntity): Future[UserEntity] = {
    db.run(users returning users += user)
  }

  def updateUser(id: Long, userUpdate: UserEntityUpdate): Future[Option[UserEntity]] = getUserById(id).flatMap {
    case Some(user) =>
      val updatedUser = userUpdate.merge(user)
      db.run(users.filter(_.id === id).update(updatedUser)).map(_ => Some(updatedUser))
    case None => Future.successful(None)
  }

  def deleteUser(id: Long): Future[Int] = db.run(users.filter(_.id === id).delete)

  def deleteAllUsers = db.run(users.delete)
}
