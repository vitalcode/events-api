package uk.vitalcode.events.api.models.db

import uk.vitalcode.events.api.models.UserEntity
import uk.vitalcode.events.api.utils.DatabaseService

trait UserEntityTable {

  protected val databaseService: DatabaseService

  import databaseService.driver.api._

  class Users(tag: Tag) extends Table[UserEntity](tag, "users") {
    def id = column[Option[Long]]("id", O.PrimaryKey, O.AutoInc)

    def username = column[String]("username")

    def password = column[String]("password")

    def permissions = column[Option[String]]("permissions")

    def * = (id, username, password, permissions) <>((UserEntity.apply _).tupled, UserEntity.unapply)
  }

  protected val users = TableQuery[Users]
}
