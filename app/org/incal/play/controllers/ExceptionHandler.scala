package org.incal.play.controllers

import java.util.concurrent.TimeoutException

import org.incal.play.util.WebUtil.redirectToRefererOrElse
import play.api.Logger
import play.api.mvc.{Call, Request, Result}
import play.api.mvc.Results.{InternalServerError, Redirect}

trait ExceptionHandler {

  protected def homeCall: Call

  protected def referrerOrHome(
    useReferrer: Boolean = true)(
    implicit request: Request[_]
  ): Result =
    if (useReferrer) redirectToRefererOrElse(homeCall) else Redirect(homeCall)

  protected def handleExceptions(
    functionName: String,
    extraMessage: Option[String] = None)(
    implicit request: Request[_]
  ): PartialFunction[Throwable, Result] = {
    case _: TimeoutException =>
      handleTimeoutException(functionName, extraMessage)

    case e: Throwable =>
      handleFatalException(functionName, extraMessage, e)
  }

  protected def handleTimeoutException(
    functionName: String,
    extraMessage: Option[String] = None,
    useReferrer: Boolean = true)(
    implicit request: Request[_]
  ): Result = {
    val message = s"The request timed out while executing $functionName function${extraMessage.getOrElse("")}."
    Logger.error(message)
    referrerOrHome(useReferrer).flashing("errors" -> message)
  }

  protected def handleBusinessException(
    message: String,
    e: Throwable,
    useReferrer: Boolean = true)(
    implicit request: Request[_]
  ): Result = {
    Logger.error(message, e)
    referrerOrHome(useReferrer).flashing("errors" -> message)
  }

  protected def handleFatalException(
    functionName: String,
    extraMessage: Option[String],
    e: Throwable
  ): Result = {
    val message = s"Fatal error detected while executing $functionName function${extraMessage.getOrElse("")}."
    Logger.error(message, e)
    InternalServerError(e.getMessage)
  }
}