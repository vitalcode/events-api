package me.archdev.restapi.models

case class UserEntity(id: Option[Long] = None, username: String, password: String, permissions: Option[String]) {
  require(!username.isEmpty, "username.empty")
  require(!password.isEmpty, "password.empty")
}

case class UserEntityUpdate(userName: Option[String] = None, password: Option[String] = None, permissions: Option[String]) {
  def merge(user: UserEntity): UserEntity = {
    UserEntity(user.id, userName.getOrElse(user.username), password.getOrElse(user.password), user.permissions)
  }
}