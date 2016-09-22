package me.archdev.restapi.http

import sangria.execution.{FieldTag, Middleware, MiddlewareBeforeField, MiddlewareQueryContext}
import sangria.schema.Context

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
    val securityCtx = ctx.ctx

    if (requireAuth)
      securityCtx.user

    if (permissions.nonEmpty)
      securityCtx.ensurePermissions(permissions)

    continue
  }
}
