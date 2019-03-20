package org.incal.play.controllers

import play.api.data.Form
import play.twirl.api.Html

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * @author Peter Banda
  * @since 2018
  */
trait HasCreateView {

  protected type CreateViewData

  protected type CreateView = WebContext => CreateViewData => Html

  protected def getCreateViewData: Future[CreateViewData]

  protected def createView: CreateView

  protected def createViewWithContext(
    data: CreateViewData)(
    implicit context: WebContext
  ): Html = createView(context)(data)

  protected def createViewWithContext(
    implicit context: WebContext
  ): Future[Html] = getCreateViewData.map(createView(context)(_))
}

/**
  * @author Peter Banda
  * @since 2018
  */
trait HasFormCreateView[E] extends HasCreateView {

  protected def getFormCreateViewData(form: Form[E]): Future[CreateViewData]

  override protected def getCreateViewData = getFormCreateViewData(form)

  protected def form: Form[E]
}

/**
  * @author Peter Banda
  * @since 2018
  */
trait HasBasicFormCreateView[E] extends HasFormCreateView[E] {

  override protected type CreateViewData = Form[E]

  override protected def getFormCreateViewData(form: Form[E]) = Future(form)
}
