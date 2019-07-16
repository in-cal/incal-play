package org.incal.play.security

import be.objectify.deadbolt.scala.cache.HandlerCache
import be.objectify.deadbolt.scala.{AuthenticatedRequest, DeadboltActions, DeadboltHandler}
import be.objectify.deadbolt.scala.models.{PatternType, Subject}
import javax.inject.Inject
import org.incal.play.controllers.WithNoCaching
import play.api.mvc.{Action, AnyContent, BodyParser, Result}
import play.api.mvc.BodyParsers.parse
import org.incal.play.security.ActionSecurity._
import play.api.mvc.Results.BadRequest
import play.api.http.{Status => HttpStatus}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait ActionSecurity {

  @Inject protected var deadbolt: DeadboltActions = _
  @Inject protected var handlerCache: HandlerCache = _

  type USER <: Subject

  protected def currentUser(
    outputHandler: DeadboltHandler = handlerCache())(
    implicit request: AuthenticatedRequest[_]
  ): Future[Option[USER]] =
    outputHandler.getSubject(request).map {
      _.flatMap(toUser)
    }

  // current user from the request (without loading)
  protected def currentUserFromRequest(
    implicit request: AuthenticatedRequest[_]
  ): Option[USER] =
    request.subject.flatMap(toUser)

  protected def restrictAdminAny(
    noCaching: Boolean = false,
    outputHandler: DeadboltHandler = handlerCache()
  ): AuthActionTransformationAny = restrictAdmin(parse.anyContent, noCaching, outputHandler)

  protected def restrictAdmin[A](
    bodyParser: BodyParser[A],
    noCaching: Boolean = false,
    outputHandler: DeadboltHandler = handlerCache()
  ): AuthActionTransformation[A] = { action =>
    val guardedAction = deadbolt.Restrict[A](List(Array(SecurityRole.admin)), outputHandler)(bodyParser)(action)

    if (noCaching) WithNoCaching(guardedAction) else guardedAction
  }

  protected def restrictSubjectPresentAny(
    noCaching: Boolean = false,
    outputHandler: DeadboltHandler = handlerCache()
  ): AuthActionTransformationAny = restrictSubjectPresent(parse.anyContent, noCaching, outputHandler)

  protected def restrictSubjectPresent[A](
    bodyParser: BodyParser[A],
    noCaching: Boolean = false,
    outputHandler: DeadboltHandler = handlerCache()
  ): AuthActionTransformation[A] = { action =>
    val guardedAction = deadbolt.SubjectPresent[A](outputHandler)(bodyParser)(action)

    if (noCaching) WithNoCaching(guardedAction) else guardedAction
  }

  protected def restrictAdminOrPermissionAny(
    permission: String,
    noCaching: Boolean = false,
    outputHandler: DeadboltHandler = handlerCache()
  ): AuthActionTransformationAny =
    restrictRolesOrPermissionAny(List(Array(SecurityRole.admin)), Some(permission), noCaching, outputHandler)

  protected def restrictAdminOrPermission[A](
    permission: String,
    bodyParser: BodyParser[A],
    noCaching: Boolean = false,
    outputHandler: DeadboltHandler = handlerCache()
  ): AuthActionTransformation[A] =
    restrictRolesOrPermission[A](List(Array(SecurityRole.admin)), Some(permission), bodyParser, noCaching, outputHandler)

  protected def restrictRolesOrPermissionAny(
    roleGroups: List[Array[String]] = List(),
    permission: Option[String] = None,
    noCaching: Boolean = false,
    outputHandler: DeadboltHandler = handlerCache()
  ): AuthActionTransformationAny =
    restrictRolesOrPermission(roleGroups, permission, parse.anyContent, noCaching, outputHandler)

  protected def restrictRolesOrPermission[A](
    roleGroups: List[Array[String]] = List(),
    permission: Option[String] = None,
    bodyParser: BodyParser[A],
    noCaching: Boolean = false,
    outputHandler: DeadboltHandler = handlerCache()
  ): AuthActionTransformation[A] = { action =>

    // TODO: once we migrate to deadbolt >= 2.5 we can use deadbolt.Composite instead
    val authAction = if (roleGroups.nonEmpty && permission.isDefined)
      restrictOrPatternRegexAux(roleGroups, permission.get, bodyParser, outputHandler)
    else if (roleGroups.nonEmpty)
      deadbolt.Restrict(roleGroups, outputHandler)(bodyParser)_
    else if (permission.isDefined)
      deadbolt.Pattern(permission.get, PatternType.REGEX, None, outputHandler)(bodyParser)_
    else
      // no restriction needed
      AuthAction(bodyParser)

    if (noCaching) WithNoCaching(authAction(action)) else authAction(action)
  }

  protected def restrictAdminOrUserCustomAny(
    isAllowed: (USER, AuthenticatedRequest[AnyContent]) => Future[Boolean],
    noCaching: Boolean = false,
    outputHandler: DeadboltHandler = handlerCache()
  ): AuthActionTransformationAny =
    restrictAdminOrUserCustom(isAllowed, parse.anyContent, noCaching, outputHandler)

  protected def restrictAdminOrUserCustom[A](
    isAllowed: (USER, AuthenticatedRequest[A]) => Future[Boolean],
    bodyParser: BodyParser[A],
    noCaching: Boolean = false,
    outputHandler: DeadboltHandler = handlerCache()
  ): AuthActionTransformation[A] = { action =>
    val authAction = restrictChain(
      Seq(
        restrictAdmin(bodyParser, outputHandler = unauthorizedDeadboltHandler),
        restrictUserCustom(isAllowed, bodyParser, false, outputHandler)
      ),
      bodyParser
    )

    if (noCaching) WithNoCaching(authAction(action)) else authAction(action)
  }

  protected def restrictUserCustomAny(
    isAllowed: (USER, AuthenticatedRequest[AnyContent]) => Future[Boolean],
    noCaching: Boolean = false,
    outputHandler: DeadboltHandler = handlerCache()
  ): AuthActionTransformationAny = restrictUserCustom(isAllowed, parse.anyContent, noCaching, outputHandler)

  protected def restrictUserCustom[A](
    isAllowed: (USER, AuthenticatedRequest[A]) => Future[Boolean],
    bodyParser: BodyParser[A],
    noCaching: Boolean = false,
    outputHandler: DeadboltHandler = handlerCache()
  ): AuthActionTransformation[A] = { action =>
    val guardedAction = AuthAction[A](bodyParser) { implicit request =>
      for {
        user <- currentUser(outputHandler)
        allowed <- user.map(user =>
          isAllowed(user, request)
        ).getOrElse(
          // no user found => not-allowed
          Future(false)
        )
        result <- if (allowed) action.apply(request) else outputHandler.onAuthFailure(request)
      } yield
        result
    }

    if (noCaching) WithNoCaching(guardedAction) else guardedAction
  }

  private def restrictOrPatternRegexAux[A](
    roleGroups: List[Array[String]],
    permission: String,
    bodyParser: BodyParser[A],
    outputHandler: DeadboltHandler
  ): AuthActionTransformation[A] =
    restrictChain(
      Seq(
        deadbolt.Restrict(roleGroups, unauthorizedDeadboltHandler)(bodyParser) _,
        deadbolt.Pattern(permission, PatternType.REGEX, None, outputHandler)(bodyParser) _
      ),
      bodyParser
    )

  protected def restrictChainAny(
    restrictions: Seq[AuthActionTransformationAny]
  ) = restrictChain(restrictions, parse.anyContent)

  protected def restrictChain[A](
    restrictions: Seq[AuthActionTransformation[A]],
    bodyParser: BodyParser[A]
  ): AuthActionTransformation[A] =
    restrictChainFuture[A](
      restrictions.map { restriction =>
        action: AuthenticatedAction[A] => Future(restriction(action))
      },
      bodyParser
    )

  protected def restrictChainFutureAny(
    restrictions: Seq[AuthActionFutureTransformationAny]
  ) = restrictChainFuture(restrictions, parse.anyContent)

  protected def restrictChainFuture[A](
    restrictions: Seq[AuthActionFutureTransformation[A]],
    bodyParser: BodyParser[A]
  ): AuthActionTransformation[A] = { action: AuthenticatedAction[A] =>
    Action.async(bodyParser) { implicit request => // action.parser)
      def isAuthorized(result: Result) = result.header.status != HttpStatus.UNAUTHORIZED

      val authorizedResultFuture =
        restrictions.foldLeft(
          Future((false, BadRequest("No restriction found")))
        ) {
          (resultFuture, restriction) =>
            for {
              (authorized, result) <- resultFuture

              nextResult <- if (authorized)
                Future((authorized, result))
              else
                restriction(action).flatMap {
                  _(request).map( newResult =>
                    (isAuthorized(newResult), newResult)
                  )
                }
            } yield
              nextResult
        }

      authorizedResultFuture.map { case (_, result) => result }
    }
  }

  protected def unauthorizedDeadboltHandler =
    handlerCache.apply(DeadboltHandlerKeys.unauthorizedStatus)

  protected def defaultDeadboltHandler =
    handlerCache.apply(DeadboltHandlerKeys.default)

  private def toUser(subject: Subject): Option[USER] =
    subject match {
      case x: USER => Some(x)
      case _ => None
    }
}

object ActionSecurity {
  type AuthenticatedAction[A] = AuthenticatedRequest[A] => Future[Result]
  type AuthenticatedActionAny = AuthenticatedAction[AnyContent]

  type AuthActionTransformation[A] = AuthenticatedAction[A] => Action[A]
  type AuthActionTransformationAny = AuthActionTransformation[AnyContent]

  type AuthActionFutureTransformation[A] = AuthenticatedAction[A] => Future[Action[A]]
  type AuthActionFutureTransformationAny = AuthActionFutureTransformation[AnyContent]
}