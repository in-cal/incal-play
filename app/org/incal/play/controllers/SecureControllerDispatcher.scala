package org.incal.play.controllers

import be.objectify.deadbolt.scala.DeadboltHandler
import org.incal.play.security.ActionSecurity.AuthActionTransformationAny
import org.incal.play.security.{ActionSecurity, AuthAction}
import play.api.routing.Router.Tags._
import org.incal.play.security.SecurityUtil.toAuthenticatedAction

import scala.collection.mutable.{Map => MMap}

/**
  * Controller dispatcher secured by the Deadbolt.
  *
  * @author Peter Banda
  * @since 2018
  */
abstract class SecureControllerDispatcher[C](controllerParamId: String) extends ControllerDispatcher[C](controllerParamId) with ActionSecurity {

  protected val actionNameMap = MMap[(String, String), AuthActionTransformationAny]()

  protected def getAllowedRoleGroups(
    controllerId:
    String, actionName: String
  ): List[Array[String]] = List()

  protected def getPermission(
    controllerId: String,
    actionName: String
  ): Option[String] = None

  override protected def dispatch = dispatchAux()

  protected def dispatchAjax = dispatchAux(unauthorizedDeadboltHandler)

  private def dispatchAux(
    outputHandler: DeadboltHandler = handlerCache()
  ): DispatchActionTransformation = { cAction =>
    AuthAction { request =>
      val controllerId = getControllerId(request)
      val controller = getController(controllerId)
      val actionName = request.tags.get(RouteActionMethod).get

      val actionTransformation = actionNameMap.getOrElseUpdate(
        (controllerId, actionName),
        restrictAny(controllerId, actionName, outputHandler)
      )
      val autAction = toAuthenticatedAction(cAction(controller))
      actionTransformation(autAction)(request)
    }
  }

  protected def restrictAny(
    controllerId: String,
    actionName: String,
    outputHandler: DeadboltHandler = handlerCache()
  ): AuthActionTransformationAny = {
    val roleGroups = getAllowedRoleGroups(controllerId, actionName)
    val permission = getPermission(controllerId, actionName)

    restrictRolesOrPermissionAny(roleGroups, permission, outputHandler)
  }
}

abstract class StaticSecureControllerDispatcher[C](controllerParamId: String, controllers : Iterable[(String, C)]) extends SecureControllerDispatcher[C](controllerParamId) {

  private val idControllerMap = controllers.toMap

  override protected def getController(controllerId: String): C =
    idControllerMap.getOrElse(
      controllerId,
      throw new IllegalArgumentException(s"Controller id '${controllerId}' not recognized.")
    )
}