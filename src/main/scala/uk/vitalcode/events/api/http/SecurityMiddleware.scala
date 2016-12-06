package uk.vitalcode.events.api.http

import sangria.execution.{FieldTag, Middleware, MiddlewareBeforeField, MiddlewareQueryContext}
import sangria.schema.Context
import uk.vitalcode.events.api.models.{AuthenticationException, AuthorisationException}

import scala.concurrent.Await
import scala.concurrent.duration.Duration

case object Authorised extends FieldTag

case class Permission(name: String) extends FieldTag

object SecurityMiddleware extends Middleware[AuthContext] with MiddlewareBeforeField[AuthContext] {
  type QueryVal = Unit
  type FieldVal = Unit

  def beforeQuery(context: MiddlewareQueryContext[AuthContext, _, _]) = ()

  def afterQuery(queryVal: QueryVal, context: MiddlewareQueryContext[AuthContext, _, _]) = ()

  def beforeField(queryVal: QueryVal, mctx: MiddlewareQueryContext[AuthContext, _, _], ctx: Context[AuthContext, _]) = {
    val permissions = ctx.field.tags.collect { case Permission(p) ⇒ p }
    val requireAuth = ctx.field.tags.contains(Authorised)
    val securityCtx = ctx.ctx

    if (requireAuth) authenticateUser(securityCtx)

    if (permissions.nonEmpty) ensurePermissions(permissions, securityCtx)

    continue
  }

  private def authenticateUser(ctx: AuthContext) = {
    ctx.subject.fold(throw AuthenticationException("Invalid token"))(identity)
  }

  private def ensurePermissions(permissions: List[String], ctx: AuthContext): Unit =
    ctx.subject.fold(throw AuthenticationException("Invalid token")) {
      user ⇒
        if (!permissions.forall(user.permissions.contains))
          throw AuthorisationException("You do not have permission to perform this operation")
    }
}
