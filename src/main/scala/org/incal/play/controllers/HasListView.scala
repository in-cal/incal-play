package org.incal.play.controllers

import org.incal.core.FilterCondition
import org.incal.play.Page
import play.twirl.api.Html
import play.api.mvc.Request

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * @author Peter Banda
  * @since 2018
  */
trait HasListView[E] {

  protected type ListViewData

  protected type ListView = WebContext => ListViewData => Html

  protected def getListViewData(
    page: Page[E],
    conditions: Seq[FilterCondition]
  ): Request[_] => Future[ListViewData]

  protected def listView: ListView

  protected def listViewWithContext(
    data: ListViewData)(
    implicit context: WebContext
  ) =
    listView(context)(data)
}

/**
  * @author Peter Banda
  * @since 2018
  */
trait HasBasicListView[E] extends HasListView[E] {

  override protected type ListViewData = (Page[E], Seq[FilterCondition])

  override protected def getListViewData(
    page: Page[E],
    conditions: Seq[FilterCondition]
  ) = { _ => Future((page, conditions))}
}
