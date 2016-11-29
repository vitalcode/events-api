package uk.vitalcode.events.api.http

import sangria.execution.{FieldTag, Middleware, MiddlewareBeforeField, MiddlewareQueryContext}
import sangria.schema.Context
import uk.vitalcode.events.api.models.AuthorisationException

import scala.concurrent.Await
import scala.concurrent.duration.Duration

case object Authorised extends FieldTag

case class Permission(name: String) extends FieldTag

object SecurityMiddleware extends Middleware[EventContext] with MiddlewareBeforeField[EventContext] {
  type QueryVal = Unit
  type FieldVal = Unit

  def beforeQuery(context: MiddlewareQueryContext[EventContext, _, _]) = ()

  def afterQuery(queryVal: QueryVal, context: MiddlewareQueryContext[EventContext, _, _]) = ()

  def beforeField(queryVal: QueryVal, mctx: MiddlewareQueryContext[EventContext, _, _], ctx: Context[EventContext, _]) = {
    val permissions = ctx.field.tags.collect { case Permission(p) ⇒ p }
    val requireAuth = ctx.field.tags.contains(Authorised)
    val token = ctx.ctx.token
    val securityCtx = ctx.ctx

    if (requireAuth)
      authenticateUser(securityCtx)

    if (permissions.nonEmpty)
      ensurePermissions(permissions, securityCtx)

    continue
  }

  private def authenticateUser(ctx: EventContext) = {
    ctx.token.flatMap(t => Await.result(ctx.authService.authorise(t.token), Duration.Inf)).fold(throw AuthorisationException("Invalid token (SecurityMiddleware)"))(identity)
  }

  def ensurePermissions(permissions: List[String], ctx: EventContext): Unit =
    ctx.token.flatMap(t => Await.result(ctx.authService.authorise(t.token), Duration.Inf)).fold(throw AuthorisationException("Invalid token (ensurePermissions)")) {
      user ⇒
        if (!permissions.forall(user.permissions.contains))
          throw AuthorisationException("You do not have permission to perform this operation")
    }
}
