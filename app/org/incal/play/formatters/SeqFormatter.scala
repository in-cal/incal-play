package org.incal.play.formatters

import play.api.data.FormError
import play.api.data.format.Formatter

/**
  * @author Peter Banda
  * @since 2018
  */
final class SeqFormatter[T](
    fromString: String => Option[T],
    toString: T => String = (x: T) => x.toString, // by default call toString
    delimiter: String = ","                       // use comma as a default delimiter
  ) extends Formatter[Seq[T]] {

  def bind(key: String, data: Map[String, String]) =
    try {
      data.get(key).map { string =>
        val items = string.split(delimiter, -1).map(_.trim).filter(_.nonEmpty).flatMap(fromString(_)).toSeq
        Right(items)
      }.getOrElse(
        Left(List(FormError(key, s"No value found for the key '$key'")))
      )
    } catch {
      case e: Exception => Left(List(FormError(key, e.getMessage)))
    }

  def unbind(key: String, list: Seq[T]) =
    Map(key -> list.map(toString).mkString(s"$delimiter "))
}

object SeqFormatter {

  def apply: Formatter[Seq[String]] = new SeqFormatter[String](x => Some(x))

  def asInt: Formatter[Seq[Int]] = new SeqFormatter[Int](x => try {
    Some(x.toInt)
  } catch {
    case e: NumberFormatException => None
  })

  def asDouble: Formatter[Seq[Double]] = new SeqFormatter[Double](x => try {
    Some(x.toDouble)
  } catch {
    case e: NumberFormatException => None
  })
}