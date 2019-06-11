package org.incal.play.controllers

import be.objectify.deadbolt.scala.AuthenticatedRequest
import play.api.data.Form
import play.twirl.api.Html

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * @author Peter Banda
  * @since 2018
  */
trait HasEditView[E, ID] {

  protected type EditViewData

  protected type EditView = WebContext => EditViewData => Html

  protected def getEditViewData(id: ID, item: E): AuthenticatedRequest[_] => Future[EditViewData]

  protected def editView: EditView

  protected def editViewWithContext(
    data: EditViewData)(
    implicit context: WebContext
  ) = editView(context)(data)
}

/**
  * @author Peter Banda
  * @since 2018
  */
trait HasFormEditView[E, ID] extends HasEditView[E, ID] {

  protected def getFormEditViewData(id: ID, form: Form[E]): AuthenticatedRequest[_] => Future[EditViewData]

  override protected def getEditViewData(id: ID, item: E) = getFormEditViewData(id, fillForm(item))

  protected def fillForm(item: E): Form[E]
}

/**
  * @author Peter Banda
  * @since 2018
  */
trait HasBasicFormEditView[E, ID] extends HasFormEditView[E, ID] {

  override protected type EditViewData = IdForm[ID, E]

  override protected def getFormEditViewData(id: ID, form: Form[E]) = { _ => Future(IdForm(id, form)) }
}