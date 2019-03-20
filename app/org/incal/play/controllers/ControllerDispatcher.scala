package org.incal.play.controllers

import play.api.mvc.{Action, AnyContent, Controller, Request}

/**
 * Simple dispatcher using controller id lookup to find a corresponding controller to redirect a call (function) to.
 *
 * @author Peter Banda
 * @since 2018
 */
abstract class ControllerDispatcher[C](controllerParamId: String) extends Controller {

  private val noActionCaching = true

  protected def getController(controllerId: String): C

  protected def dispatch(
    action: C => Action[AnyContent]
  ): Action[AnyContent] = {
    val resultAction = Action.async { implicit request =>
      val controllerId = getControllerId(request)
      action(getController(controllerId)).apply(request)
    }

    if (noActionCaching)
      WithNoCaching(resultAction)
    else
      resultAction
  }

  // a helper function
  protected def getControllerId(request: Request[AnyContent]) = {
    val controllerIdOption = request.queryString.get(controllerParamId)
    controllerIdOption.getOrElse(
      throw new IllegalArgumentException(s"Controller param id '${controllerParamId}' not found.")
    ).head
  }
}

/**
  * Static controller dispatcher.
  *
  * @author Peter Banda
  * @since 2018
  */
class StaticControllerDispatcher[C](controllerParamId: String, controllers : Iterable[(String, C)]) extends ControllerDispatcher[C](controllerParamId) {

  private val idControllerMap = controllers.toMap

  override protected def getController(controllerId: String): C =
    idControllerMap.getOrElse(
      controllerId,
      throw new IllegalArgumentException(s"Controller id '${controllerId}' not recognized.")
    )
}