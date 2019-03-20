package org.incal.play.controllers

import org.incal.core.Identity
import org.incal.core.dataaccess.AsyncCrudRepo
import org.incal.play.security.AuthAction
import play.api.data.Form
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.api.mvc._

import scala.concurrent.Future

/**
  * Trait defining a controller with basic CRUD (create, read (find/get), update, and delete) operations.
  *
  * @author Peter Banda
  * @since 2018
  */
trait CrudController[ID] extends ReadonlyController[ID] {

  def create: Action[AnyContent]

  def edit(id: ID): Action[AnyContent]

  def save: Action[AnyContent]

  def update(id: ID): Action[AnyContent]

  def delete(id: ID): Action[AnyContent]
}

/**
 * Standard implementation of a CRUD controller using an asynchronous CRUD repo to access the data.
 *
 * @param E type of entity
 * @param ID type of identity of entity (primary key)
 *
 * @author Peter Banda
 * @since 2018
 */
abstract class CrudControllerImpl[E: Format, ID](
    override val repo: AsyncCrudRepo[E, ID]
  )(implicit identity: Identity[E, ID]) extends ReadonlyControllerImpl[E, ID]
    with CrudController[ID]
    with HasFormCreateView[E]
    with HasFormShowView[E, ID]
    with HasFormEditView[E, ID] {

  protected def fillForm(entity: E): Form[E] =
    form.fill(entity)

  protected def formFromRequest(implicit request: Request[AnyContent]): Form[E] =
    form.bindFromRequest

  // actions

  def create = AuthAction { implicit request =>
    getCreateViewData.map(viewData =>
      Ok(createViewWithContext(viewData))
    )
  }

  /**
    * Retrieve a single object by its id and display as editable.
    * NotFound response is generated if key does not exists.
    *
    * @param id id/ primary key of the object.
    */
  def edit(id: ID) = AuthAction { implicit request =>
    {
      for {
        // retrieve the item
        item <- repo.get(id)

        // create a view data if the item has been found
        viewData <- item.fold(
          Future(Option.empty[EditViewData])
        ) { entity =>
          getEditViewData(id, entity)(request).map(Some(_))
        }
      } yield
        item match {
          case None => NotFound(s"$entityName '${formatId(id)}' not found")
          case Some(entity) =>
            render {
              case Accepts.Html() => Ok(editViewWithContext(viewData.get))
              case Accepts.Json() => BadRequest("Edit function doesn't support JSON response. Use get instead.")
            }
        }
    }.recover(handleEditExceptions(id))
  }

  def save = save(_ => goHome)

  protected def save(redirect: Request[_] => Result) = AuthAction { implicit request =>
    formFromRequest.fold(
      formWithErrors => getFormCreateViewData(formWithErrors).map(viewData =>
        BadRequest(createViewWithContext(viewData))
      ),
      item =>
        saveCall(item).map { id =>
          render {
            case Accepts.Html() => redirect(request).flashing("success" -> s"$entityName '${formatId(id)}' has been created")
            case Accepts.Json() => Created(Json.obj("message" -> s"$entityName successfully created", "id" -> formatId(id)))
          }
        }.recover(handleSaveExceptions)
    )
  }

  protected def saveCall(item: E)(implicit request: Request[AnyContent]): Future[ID] = repo.save(item)

  def update(id: ID): Action[AnyContent] = update(id, _ => goHome)

  protected def update(id: ID, redirect: Request[_] => Result): Action[AnyContent] = AuthAction { implicit request =>
    formFromRequest.fold(
      formWithErrors => getFormEditViewData(id, formWithErrors)(request).map { viewData =>
        BadRequest(editViewWithContext(viewData))
      },
      item =>
        updateCall(identity.set(item, id)).map { _ =>
          render {
            case Accepts.Html() => redirect(request).flashing("success" -> s"$entityName '${formatId(id)}' has been updated")
            case Accepts.Json() => Ok(Json.obj("message" -> s"$entityName successly updated", "id" -> formatId(id)))
          }
        }.recover(handleUpdateExceptions(id))
    )
  }

  protected def updateCall(item: E)(implicit request: Request[AnyContent]): Future[ID] = repo.update(item)

  def delete(id: ID) = AuthAction { implicit request =>
    deleteCall(id).map { _ =>
      render {
        case Accepts.Html() => goHome.flashing("success" -> s"$entityName '${formatId(id)}' has been deleted")
        case Accepts.Json() => Ok(Json.obj("message" -> s"$entityName successfully deleted", "id" -> formatId(id)))
      }
    }.recover(handleDeleteExceptions(id))
  }

  protected def deleteCall(id: ID)(implicit request: Request[AnyContent]): Future[Unit] = repo.delete(id)

  protected def handleEditExceptions(id: ID)(implicit request: Request[_]) = handleExceptionsWithId("an edit", id)
  protected def handleSaveExceptions(implicit request: Request[_]) = handleExceptions("a save")
  protected def handleUpdateExceptions(id: ID)(implicit request: Request[_]) = handleExceptionsWithId("an update", id)
  protected def handleDeleteExceptions(id: ID)(implicit request: Request[_]) = handleExceptionsWithId("a delete", id)
}