package uk.vitalcode.events.api.http

import sangria.execution.{FieldTag, Middleware, MiddlewareBeforeField, MiddlewareQueryContext}
import sangria.schema.Context
import uk.vitalcode.events.api.models.{AuthenticationException, AuthorisationException}

import scala.concurrent.Await
import scala.concurrent.duration.Duration

case object Authorised extends FieldTag

case class Permission(name: String) extends FieldTag

object SecurityMiddleware extends Middleware[GraphqlContext] with MiddlewareBeforeField[GraphqlContext] {
  type QueryVal = Unit
  type FieldVal = Unit

  def beforeQuery(context: MiddlewareQueryContext[GraphqlContext, _, _]) = ()

  def afterQuery(queryVal: QueryVal, context: MiddlewareQueryContext[GraphqlContext, _, _]) = ()

  def beforeField(queryVal: QueryVal, mctx: MiddlewareQueryContext[GraphqlContext, _, _], ctx: Context[GraphqlContext, _]) = {
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

  private def authenticateUser(ctx: GraphqlContext) = {
    ctx.token.flatMap(t => Await.result(ctx.authService.authorise(t.token), Duration.Inf)).fold(throw AuthenticationException("Invalid token (SecurityMiddleware)"))(identity)
  }

  def ensurePermissions(permissions: List[String], ctx: GraphqlContext): Unit =
    ctx.token.flatMap(t => Await.result(ctx.authService.authorise(t.token), Duration.Inf)).fold(throw AuthenticationException("Invalid token (ensurePermissions)")) {
      user ⇒
        if (!permissions.forall(user.permissions.contains))
          throw AuthorisationException("You do not have permission to perform this operation")
    }
}
