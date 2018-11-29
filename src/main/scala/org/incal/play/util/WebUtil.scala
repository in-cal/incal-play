package org.incal.play.util

import play.api.mvc.Results.Redirect
import play.api.mvc.{AnyContent, Call, Request, Result}

object WebUtil {

  def matchesPath(coreUrl : String, url : String, matchPrefixDepth : Option[Int] = None) =
    matchPrefixDepth match {
      case Some(prefixDepth) => {
        var slashPos = 0
        for (i <- 1 to prefixDepth) {
          slashPos = coreUrl.indexOf('/', slashPos + 1)
        }
        if (slashPos == -1) {
          slashPos = coreUrl.indexOf('?')
          if (slashPos == -1)
            slashPos = coreUrl.length
        }
        val subString = coreUrl.substring(0, slashPos)
        url.startsWith(coreUrl.substring(0, slashPos))
      }
      case None => url.startsWith(coreUrl)
    }

  def getParamValue(url : String, param: String): Option[String] = {
    val tokens = url.split(param + "=")
    if (tokens.isDefinedAt(1))
      Some(tokens(1).split("&")(0))
    else
      None
  }

  def getRequestParamMap(implicit request: Request[AnyContent]): Map[String, Seq[String]] = {
    val body = request.body
    if (body.asFormUrlEncoded.isDefined)
      body.asFormUrlEncoded.get
    else if (body.asMultipartFormData.isDefined)
      body.asMultipartFormData.get.asFormUrlEncoded
    else
      throw new IllegalArgumentException("FormUrlEncoded or MultipartFormData request expected.")
  }

  def getRequestParamValueOptional(
    paramKey: String)(
    implicit request: Request[AnyContent]
  ) =
    getRequestParamMap.get(paramKey).map(_.head)

  def getRequestParamValue(
    paramKey: String)(
    implicit request: Request[AnyContent]
  ) =
    getRequestParamValueOptional(paramKey).getOrElse(
      throw new IllegalArgumentException(s"Request param with the key $paramKey not found.")
    )

  def redirectToRefererOrElse(
    call: Call)(
    implicit request: Request[_]
  ): Result =
    request.headers.get("Referer") match {
      case Some(refererUrl) => Redirect(refererUrl)
      case None => Redirect(call)
    }
}
