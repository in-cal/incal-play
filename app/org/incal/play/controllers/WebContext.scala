package org.incal.play.controllers

import be.objectify.deadbolt.scala.AuthenticatedRequest
import controllers.WebJarAssets
import play.api.Configuration
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.Flash

/**
  * Web context passed by implicits with some useful resources commonly required by Play views.
  *
  * @author Peter Banda
  * @since 2018
  */
case class WebContext(implicit val flash: Flash, val msg: Messages, val request: AuthenticatedRequest[_], val webJarAssets: WebJarAssets, val configuration: Configuration)

object WebContext {
  implicit def toFlash(
    implicit webContext: WebContext
  ): Flash = webContext.flash

  implicit def toMessages(
    implicit webContext: WebContext
  ): Messages = webContext.msg

  implicit def toRequest(
    implicit webContext: WebContext
  ): AuthenticatedRequest[_] = webContext.request

  implicit def toWebJarAssets(
    implicit webContext: WebContext
  ): WebJarAssets = webContext.webJarAssets

  implicit def toConfiguration(
    implicit webContext: WebContext
  ): Configuration = webContext.configuration

  implicit def apply(
    messagesApi: MessagesApi,
    webJarAssets: WebJarAssets,
    configuration: Configuration)(
    implicit request: AuthenticatedRequest[_]
  ): WebContext = {
    implicit val msg = messagesApi.preferred(request)
    implicit val flash = request.flash
    implicit val webJars = webJarAssets
    implicit val conf = configuration
    WebContext()
  }
}