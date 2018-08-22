package org.incal.play.security

import be.objectify.deadbolt.scala.SubjectActionBuilder
import org.incal.play.security.SecurityUtil.AuthenticatedAction
import play.api.mvc.BodyParsers.parse
import play.api.mvc.{Action, AnyContent}

object AuthAction {

  def apply(
    action: AuthenticatedAction[AnyContent]
  ): Action[AnyContent] =
    SubjectActionBuilder(None).async(parse.anyContent) { authRequest =>
      action(authRequest)
    }
}