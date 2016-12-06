package uk.vitalcode.events.api.http.schema

import sangria.schema.{Field, ObjectType, OptionType, UpdateCtx, _}
import uk.vitalcode.events.api.http.{AuthContext, Permission}
import uk.vitalcode.events.api.services.AuthService

trait Mutation {

  val authService: AuthService

  private val UserNameArg = Argument("user", StringType)
  private val PasswordArg = Argument("password", StringType)
  private val RoleArg = Argument("role", StringType)

  val MutationType = ObjectType("Mutation", fields[AuthContext, Unit](
    Field("login", OptionType(StringType),
      arguments = UserNameArg :: PasswordArg :: Nil,
      resolve = ctx => UpdateCtx(authService.login(ctx.arg(UserNameArg), ctx.arg(PasswordArg))) { token ⇒
        ctx.ctx.withToken(token)
      }
    ),
    Field("register", OptionType(StringType),
      arguments = UserNameArg :: PasswordArg :: Nil,
      tags = Permission("admin") :: Nil,
      resolve = ctx ⇒ {
        UpdateCtx(authService.signup(ctx.arg(UserNameArg), ctx.arg(PasswordArg))) { token ⇒
          ctx.ctx.withToken(token)
        }
      }
    )
  ))
}
