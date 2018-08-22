package org.incal.play.controllers

import play.api.http.HeaderNames
import play.api.mvc._
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future

/**
  * @author Peter Banda
  * @since 2018
  */
class NoCacheAction extends ActionBuilder[Request] with NoCacheSetting {

  def invokeBlock[A](
    request: Request[A],
    block: (Request[A]) => Future[Result]
  ) =
    block(request).map(_.withHeaders(HeaderNames.CACHE_CONTROL -> noCacheSetting))
}

case class WithNoCaching[A](action: Action[A]) extends Action[A] with NoCacheSetting {

  def apply(request: Request[A]): Future[Result] = {
    action(request).map(_.withHeaders(HeaderNames.CACHE_CONTROL -> noCacheSetting))
  }

  lazy val parser = action.parser
}

trait NoCacheSetting {
  protected val noCacheSetting = "no-cache, max-age=0, must-revalidate, no-store"
}