package controllers.core

import play.api.data.FormError
import play.api.data.format.Formatter

import scala.reflect.ClassTag

private class TempJavaEnumFormatter[E <: Enum[E]](clazz: Class[E]) extends Formatter[E] {

  def bind(key: String, data: Map[String, String]) = {
    try {
      data.get(key).map(value =>
        try {
          Right(Enum.valueOf[E](clazz, value))
        } catch {
          case _: IllegalArgumentException => Left(List(FormError(key, s"Enumeration expected of type: '${clazz.getName}', but it does not appear to contain the value: '$value'")))
        }
      ).getOrElse(
        Left(List(FormError(key, s"No value found for the key '$key'")))
      )
    } catch {
      case e: Exception => Left(List(FormError(key, e.getMessage)))
    }
  }

  def unbind(key: String, value: E) =
    Map(key -> value.toString)
}

private class EnumMapFormatter[E](stringValueMap: Map[String, E]) extends Formatter[E] {

  def bind(key: String, data: Map[String, String]) = {
    try {
      data.get(key).map(value =>
        stringValueMap.get(value) match {
          case Some(enum) => Right(enum)
          case None => Left(List(FormError(key, s"Map formatter does not appear to contain the value: '$value'")))
        }
      ).getOrElse(
        Left(List(FormError(key, s"No value found for the key '$key'")))
      )
    } catch {
      case e: Exception => Left(List(FormError(key, e.getMessage)))
    }
  }

  def unbind(key: String, value: E) =
    Map(key -> value.toString)
}

object TempJavaEnumFormatter {

  def apply[E <: Enum[E]](implicit classTag: ClassTag[E]): Formatter[E] = {
    val clazz = classTag.runtimeClass.asInstanceOf[Class[E]]
    apply[E](clazz)
  }

  def apply[E <: Enum[E]](clazz: Class[E]): Formatter[E] =
    new TempJavaEnumFormatter[E](clazz)

  def applyX[E](clazz: Class[E]): Formatter[E] = {
    val stringEnumMap = clazz.getEnumConstants.map(enumConst => (enumConst.toString, enumConst)).toMap
    new EnumMapFormatter[E](stringEnumMap)
  }
}