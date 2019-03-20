package org.incal.play.controllers

import javax.inject.Inject

import be.objectify.deadbolt.scala.cache.HandlerCache
import be.objectify.deadbolt.scala.models.PatternType
import be.objectify.deadbolt.scala.{DeadboltActions, DeadboltHandler}
import org.incal.play.security.DeadboltHandlerKeys
import play.api.mvc._
import play.api.routing.Router.Tags._
import org.incal.play.security.SecurityUtil.{AuthenticatedAction, restrictChain2, toActionAny, toAuthenticatedAction}

import scala.collection.mutable.{Map => MMap}

/**
  * Controller dispatcher secured by the Deadbolt.
  *
  * @author Peter Banda
  * @since 2018
  */
abstract class SecureControllerDispatcher[C](controllerParamId: String) extends ControllerDispatcher[C](controllerParamId) {

  @Inject protected var deadbolt: DeadboltActions = _
  @Inject protected var deadboltHandlerCache: HandlerCache = _

  protected val actionNameMap = MMap[(String, String), (C => Action[AnyContent]) => Action[AnyContent]]()

  protected def getAllowedRoleGroups(
    controllerId:
    String, actionName: String
  ): List[Array[String]] = List()

  protected def getPermission(
    controllerId: String,
    actionName: String
  ): Option[String] = None

  override protected def dispatch(
    action: C => Action[AnyContent]
  ) = dispatchAux(action, None)

  protected def dispatchAjax(
    action: C => Action[AnyContent]
  ) = dispatchAux(action, Some(unauthorizedDeadboltHandler))

  private def dispatchAux(
    action: C => Action[AnyContent],
    outputDeadboltHandler: Option[DeadboltHandler]
  ) = Action.async { implicit request =>
    val controllerId = getControllerId(request)
    val actionName = request.tags.get(RouteActionMethod).get

    val restrictedAction = actionNameMap.getOrElseUpdate(
      (controllerId, actionName),
      createRestrictedAction(controllerId, actionName, outputDeadboltHandler)_
    )
    restrictedAction(action).apply(request)
  }

  protected def createRestrictedAction(
    controllerId: String,
    actionName: String,
    outputDeadboltHandler: Option[DeadboltHandler] = None
  )(
    action: C => Action[AnyContent]
  ): Action[AnyContent] = {
    val roleGroups = getAllowedRoleGroups(controllerId, actionName)
    val permission = getPermission(controllerId, actionName)

    // TODO: once we migrate to deadbolt >= 2.5 we can use deadbolt.Composite instead

    val handler = outputDeadboltHandler.getOrElse(deadboltHandlerCache())

    val actionTransform: AuthenticatedAction[AnyContent] => Action[AnyContent] =
      if (roleGroups.nonEmpty && permission.isDefined)
        RestrictOrPattern(roleGroups, permission.get, outputDeadboltHandler)_
      else if (roleGroups.nonEmpty)
        deadbolt.Restrict[AnyContent](roleGroups, handler)()_
      else if (permission.isDefined)
        deadbolt.Pattern[AnyContent](permission.get, PatternType.REGEX, None, handler)()_
      else
        // no deadbolt action needed
        {authenticatedAction => toActionAny(authenticatedAction)} //  identity[Action[AnyContent]]_

    actionTransform(dispatchAuthenticated(action))
  }

  protected def dispatchAuthenticated(
    action: C => Action[AnyContent]
  ): AuthenticatedAction[AnyContent] =
    toAuthenticatedAction(super.dispatch(action))

  private def RestrictOrPattern(
    roleGroups: List[Array[String]],
    permission: String,
    outputDeadboltHandler: Option[DeadboltHandler] = None)(
    action: AuthenticatedAction[AnyContent]
  ): Action[AnyContent] = {
    val handler = outputDeadboltHandler.getOrElse(deadboltHandlerCache())

    restrictChain2(Seq(
      deadbolt.Restrict[AnyContent](roleGroups, unauthorizedDeadboltHandler)() _,
      deadbolt.Pattern[AnyContent](permission, PatternType.REGEX, None, handler)() _
    ))(action)
  }

  //  private def RestrictOrPattern[A](
//    roleGroups: List[Array[String]],
//    permission: String)(
//    action: AuthenticatedAction[A]
//  ): Action[A] =
//    SecurityUtil.restrictChain[A](Seq(
//      deadbolt.Restrict[A](roleGroups, unauthorizedDeadboltHandler)()_,
//      deadbolt.Pattern[A](permission, PatternType.REGEX)()_
//    ))(action)

  protected def unauthorizedDeadboltHandler =
    deadboltHandlerCache.apply(DeadboltHandlerKeys.unauthorizedStatus)

  protected def defaultDeadboltHandler =
    deadboltHandlerCache.apply(DeadboltHandlerKeys.default)
}

abstract class StaticSecureControllerDispatcher[C](controllerParamId: String, controllers : Iterable[(String, C)]) extends SecureControllerDispatcher[C](controllerParamId) {

  private val idControllerMap = controllers.toMap

  override protected def getController(controllerId: String): C =
    idControllerMap.getOrElse(
      controllerId,
      throw new IllegalArgumentException(s"Controller id '${controllerId}' not recognized.")
    )
}