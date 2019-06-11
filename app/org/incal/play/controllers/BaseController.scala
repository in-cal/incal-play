package org.incal.play.controllers

import javax.inject.Inject
import be.objectify.deadbolt.scala.AuthenticatedRequest
import controllers.WebJarAssets
import org.incal.play.security.ActionSecurity
import play.api.Configuration
import play.api.i18n.MessagesApi
import play.api.mvc.Controller

/**
  * Base controller with injected resources such as message API, web jars, and configurations, commonly used for Play controllers. .
  *
  * @author Peter Banda
  * @since 2018
  */
trait BaseController extends Controller with ActionSecurity {

  @Inject protected var messagesApi: MessagesApi = _
  @Inject protected var webJarAssets: WebJarAssets = _
  @Inject protected var configuration: Configuration = _

  protected implicit def webContext(implicit request: AuthenticatedRequest[_]) =
    WebContext(messagesApi, webJarAssets, configuration)(request)
}