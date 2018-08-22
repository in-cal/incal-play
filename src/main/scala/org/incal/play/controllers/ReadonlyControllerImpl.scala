package org.incal.play.controllers

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.api.mvc._
import org.incal.core.dataaccess._
import org.incal.core.FilterCondition
import org.incal.core.FilterCondition.toCriterion
import org.incal.play.Page
import org.incal.play.security.AuthAction

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

/**
  * Trait defining a controller with basic read (find/get) operations, i.e., no data alternation is allowed.
  *
  * @author Peter Banda
  * @since 2018
  */
trait ReadonlyController[ID] {

  def get(id: ID): Action[AnyContent]

  def find(page: Int, orderBy: String, filter: Seq[FilterCondition]): Action[AnyContent]

  def listAll(orderBy: String): Action[AnyContent]
}

/**
 * Standard implementation of a readonly controller using an asynchronous readonly repo to access the data.
 *
 * @param E type of entity
 * @param ID type of identity of entity (primary key)
 *
 * @author Peter Banda
 * @since 2018
 */
abstract class ReadonlyControllerImpl[E: Format, ID] extends BaseController
  with ReadonlyController[ID]
  with HasListView[E]
  with HasShowView[E, ID]
  with ExceptionHandler {

  protected val pageLimit = 20

  protected val timeout = 200 seconds

  protected def homeCall: Call

  protected def goHome = Redirect(homeCall)

  protected def repo: AsyncReadonlyRepo[E, ID]

  protected def repoHook = repo

  protected def listViewColumns: Option[Seq[String]] = None

  protected def entityNameKey = ""
  protected lazy val entityName = if (entityNameKey.isEmpty) "Item" else messagesApi.apply(entityNameKey + ".displayName")

  protected def formatId(id: ID) = id.toString

  /**
   * Retrieve single object by its Id.
   * NotFound response is generated if key does not exists.
   *
   * @param id id/ primary key of the object.
   */
  def get(id: ID) = AuthAction { implicit request =>
    {
      for {
        // retrieve the item
        item <- repo.get(id)

        // create a view data if the item has been found
        viewData <- item.fold(
          Future(Option.empty[ShowViewData])
        ) { entity =>
          getShowViewData(id, entity)(request).map(Some(_))
        }
      } yield
        item match {
          case None => NotFound(s"$entityName #${formatId(id)} not found")
          case Some(entity) =>
            render {
              case Accepts.Html() => Ok(showViewWithContext(viewData.get))
              case Accepts.Json() => Ok(Json.toJson(entity))
            }
        }
    }.recover(handleGetExceptions(id))
  }

  /**
    * Display the paginated list.
    *
    * @param page Current page number (starts from 0)
    * @param orderBy Column to be sorted
    * @param conditions Filter applied on items
    */
  def find(
    page: Int,
    orderBy: String,
    conditions: Seq[FilterCondition]
  ) = AuthAction { implicit request =>
    {
      for {
        (items, count) <- getFutureItemsAndCount(page, orderBy, conditions)

        viewData <- getListViewData(
          Page(items, page, page * pageLimit, count, orderBy),
          conditions
        )(request)
      } yield
        render {
          case Accepts.Html() => Ok(listViewWithContext(viewData))
          case Accepts.Json() => Ok(Json.toJson(items))
        }
    }.recover(handleFindExceptions)
  }

  /**
   * Display all items in a paginated fashion.
   *
   * @param orderBy Column to be sorted
   */
  def listAll(orderBy: String) = AuthAction { implicit request =>
    {
      for {
        (items, count) <- getFutureItemsAndCount(None, orderBy, Nil, Nil, None)

        viewData <- getListViewData(
          Page(items, 0, 0, count, orderBy),
          Nil
        )(request)
      } yield
        render {
          case Accepts.Html() => Ok(listViewWithContext(viewData))
          case Accepts.Json() => Ok(Json.toJson(items))
        }
    }.recover(handleListAllExceptions)
  }

  protected def getFutureItemsAndCount(
    page: Int,
    orderBy: String,
    filter: Seq[FilterCondition]
  ): Future[(Traversable[E], Int)] =
    getFutureItemsAndCount(Some(page), orderBy, filter, listViewColumns.getOrElse(Nil), Some(pageLimit))

  protected def getFutureItemsAndCount(
    page: Option[Int],
    orderBy: String,
    filter: Seq[FilterCondition],
    projection: Seq[String],
    limit: Option[Int]
  ): Future[(Traversable[E], Int)] = {
    val sort = toSort(orderBy)
    val skip = page.zip(limit).headOption.map { case (page, limit) =>
      page * limit
    }

    for {
      criteria <- toCriteria(filter)
      itemsCount <- {
        val itemsFuture = repo.find(criteria, sort, projection, limit, skip)
        val countFuture = repo.count(criteria)

        for { items <- itemsFuture; count <- countFuture} yield
          (items, count)
      }
    } yield
      itemsCount
  }

  protected def getFutureItems(
    page: Option[Int],
    orderBy: String,
    filter: Seq[FilterCondition],
    projection: Seq[String],
    limit: Option[Int]
  ): Future[Traversable[E]] = {
    val sort = toSort(orderBy)
    val skip = page.zip(limit).headOption.map { case (page, limit) =>
      page * limit
    }

    toCriteria(filter).flatMap ( criteria =>
      repo.find(criteria, sort, projection, limit, skip)
    )
  }

  protected def getFutureItemsForCriteria(
    page: Option[Int],
    orderBy: String,
    criteria: Seq[Criterion[Any]],
    projection: Seq[String],
    limit: Option[Int]
  ): Future[Traversable[E]] = {
    val sort = toSort(orderBy)
    val skip = page.zip(limit).headOption.map { case (page, limit) =>
      page * limit
    }

    repo.find(criteria, sort, projection, limit, skip)
  }

  protected def getFutureCount(filter: Seq[FilterCondition]): Future[Int] =
    toCriteria(filter).flatMap(repo.count)

  protected def toCriteria(
    filter: Seq[FilterCondition]
  ): Future[Seq[Criterion[Any]]] = {
    val fieldNames = filter.seq.map(_.fieldName)

    filterValueConverters(fieldNames).map( valueConverters =>
      filter.map(toCriterion(valueConverters)).flatten
    )
  }

  protected def filterValueConverters(
    fieldNames: Traversable[String]
  ): Future[Map[String, String => Option[Any]]] =
    Future(Map())

  /**
    * TODO: Move this into utility object.
    * Convert String into a Sort class.
    *
    * @param fieldName Reference column sorting.
    * @return Option with Sort class indicating an order (asc/desc). None, if string is empty.
    */
  protected def toSort(fieldName : String): Seq[Sort] =
    if (fieldName.nonEmpty) {
      val sort = if (fieldName.startsWith("-"))
        DescSort(fieldName.substring(1, fieldName.length))
      else
        AscSort(fieldName)
      Seq(sort)
    } else
      Nil

  protected def result[T](future: Future[T]): T =
    Await.result(future, timeout)

  protected def handleGetExceptions(id: ID)(implicit request: Request[_]) = handleExceptionsWithId("a get", id)
  protected def handleFindExceptions(implicit request: Request[_]) = handleExceptions("a find")
  protected def handleListAllExceptions(implicit request: Request[_]) = handleExceptions("a list-all")

  protected def handleExceptionsWithId(
    functionName: String,
    id: ID)(
    implicit request: Request[_]
  ): PartialFunction[Throwable, Result] = {
    val idPart = s" for the item with id '${formatId(id)}'"
    handleExceptions(functionName, Some(idPart))
  }
}