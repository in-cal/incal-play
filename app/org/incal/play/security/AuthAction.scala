package org.incal.play.security

import be.objectify.deadbolt.scala.{SubjectActionBuilder}
import play.api.mvc.BodyParsers.parse
import play.api.mvc.{AnyContent, BodyParser}
import ActionSecurity._

object AuthAction {
  def apply: AuthActionTransformationAny = apply[AnyContent](parse.anyContent)

  def apply[A](
    bodyParser: BodyParser[A]
  ): AuthActionTransformation[A] = { action =>
    SubjectActionBuilder(None).async(bodyParser) { authRequest =>
      action(authRequest)
    }
  }
}