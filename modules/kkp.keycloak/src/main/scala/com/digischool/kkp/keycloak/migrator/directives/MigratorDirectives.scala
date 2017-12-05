package com.digischool.kkp.keycloak.migrator.directives

import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.HttpCookiePair
import akka.http.scaladsl.server.RouteResult.Complete
import akka.http.scaladsl.server._
import com.digischool.kkp.core.injectable.{WithExecutionContext, WithHttpExt, WithMaterializer, WithNameableLog}
import com.digischool.kkp.core.models.config.HostConfiguration
import com.digischool.kkp.core.utils.{HttpUtils, PlayJsonSupport}
import com.digischool.kkp.keycloak.models.UserRepresentation
import com.digischool.kkp.keycloak.models.config.{MigrationTargets, MigratorConfig}
import com.digischool.kkp.keycloak.services.{TokenHandler, UserAdmin}
import com.kreactive.model.UserId
import org.keycloak.adapters.KeycloakDeployment
import org.keycloak.jose.jws.JWSInput
import play.api.libs.json.{JsObject, Json}

import scala.collection.immutable.Seq
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Success, Try}

/**
  * A rejection when first call to authenticate using keycloak failed.
  *
  * @param r the original response from keycloak, to return if we weren't able to recover it
  */
case class LoginFailedRejection(r: HttpResponse) extends Rejection

trait MigratorDirectives extends
  TokenHandler with
  PlayJsonSupport with
  HttpUtils with
  UserAdmin {
  self: WithNameableLog with
    WithHttpExt with
    WithExecutionContext with
    WithMaterializer =>

  import Directives._

  private val log = logAs(getClass)

  case class LoginForm(username: String, password: String)

  private def loginPath(realm: String): Directive0 = post &
    pathPrefix("auth") &
    pathPrefix("realms") &
    pathPrefix(realm) &
    pathPrefix("login-actions") &
    pathPrefix("authenticate")


  def loginAction(mc: MigratorConfig, hc: HostConfiguration): Directive0 = {
    val realm = mc.deployment.getRealm
    val eligible: Directive[(String, MigrationTargets)] =
      loginPath(realm) & checkClientId(mc.targets)

    eligible.tflatMap { case (clientid, target) => toStrictEntity(60.seconds) & logit(mc.deployment, hc, clientid, target) } | pass

  }

  private def checkClientId(targets: List[MigrationTargets]): Directive[(String, MigrationTargets)] = cookie("KC_RESTART").map { (pair: HttpCookiePair) =>
    (for {
      json <- Try(Json.parse(new JWSInput(pair.value).readContentAsString)).toOption
      decoded <- json.asOpt[JsObject]
      clientid <- (decoded \ "cid").asOpt[String]
      target <- targets.find(_.clientids.contains(clientid))
    } yield (clientid, target)
      ).getOrElse {
      reject(MissingCookieRejection("KC_RESTART"))
      ("", MigrationTargets(Nil, Nil, Nil))
    }
  } & cancelRejections(classOf[MissingCookieRejection])


  def logit(deployment: KeycloakDeployment, hc: HostConfiguration, clientId: String, target: MigrationTargets): Directive0 = {
    flagDrainedEntity(as[FormData]).flatMap { form =>
      val loginForm = LoginForm(form.fields.toMap("username"), form.fields.toMap("password"))
      mapRouteResultPF {
        // Ok means login failed so a newlogin form is provided
        case Complete(r@HttpResponse(StatusCodes.OK, _, _, _)) =>
          RouteResult.Rejected(Seq(LoginFailedRejection(r)))
      } recoverPF {
        case Seq(LoginFailedRejection(r)) => responseHandler(deployment, loginForm, r, target)
      } recoverPF {
        case Seq(LoginFailedRejection(r)) => complete(r)
      }
    }
  }


  private def responseHandler(deployment: KeycloakDeployment, form: LoginForm, response: HttpResponse, target: MigrationTargets): Directive0 = {
    val onlyPassword = target.updatepasswordonly
    val hooks = target.forwardurls
    val checkSources = target.wsurls
    onComplete(check(checkSources, form)) flatMap { jsonOT =>
      val maybe = for {
        json <- jsonOT.toOption.flatten
        user <- json.asOpt[UserRepresentation]
      } yield retry(user, form, deployment, onlyPassword, response) flatMap (hookOnReturn(_, json, hooks))
      maybe.getOrElse(reject(LoginFailedRejection(response)): Directive0)
    }
  }

  private def hookOnReturn(userId: UserId, json: JsObject, hooks: List[String]): Directive0 = mapRouteResult { r =>
    callHooks(userId, json, hooks)
    r
  }

  private def retry(user: UserRepresentation, form: LoginForm, deployment: KeycloakDeployment, onlyPassword: Boolean, firstResponse: HttpResponse): Directive1[UserId] = {
    val updatedWithPassword = user.withPassword(form.password)
    onComplete(if (onlyPassword) updateUserPassword(form.username, form.password, deployment) else createAndResetPassword(updatedWithPassword, deployment)) flatMap {
      case Success(userId) => provide(userId)
      case _ => reject(LoginFailedRejection(firstResponse)): Directive1[UserId]
    }
  }

  def callHooks(userid: UserId, user: JsObject, hooks: List[String]): Future[Unit] = {
    val updatedUser = user ++ Json.obj("id" -> userid)
    val fEntity = Marshal(updatedUser).to[RequestEntity]
    for {
      entity <- fEntity
      responses = Future.traverse(hooks)(callHook(_, entity, userid))
    } yield {
      ()
    }
  }

  private def check(sources: List[String], form: LoginForm): Future[Option[JsObject]] = {
    val formData = FormData("username" -> form.username, "password" -> form.password)
    Future.traverse(sources)(doReq(_, formData)).map(listTry => listTry.collect { case Success(x) => x }.headOption)
  }

  private def callHook(url: String, entity: RequestEntity, userid: UserId): Future[Boolean] = {
    val fResponse = requestAndDrain(HttpRequest(uri = url, method = HttpMethods.POST, entity = entity))
    fResponse.map {
      case HttpResponse(StatusCodes.OK, _, _, _) => true
      case _ => log.error(s"bad response to hook $url for user $userid : ")
        false
    }.recover { case e => log.error(s"unable to call hook $url for user $userid : " + e); false }
  }

  private def doReq(source: String, formData: FormData): Future[Try[JsObject]] = {
    val fEntity = Marshal(formData).to[RequestEntity]
    val fCheckResp = for {
      entity <- fEntity
      response <- requestAndUnmarshalOK[JsObject](HttpRequest(uri = source, method = HttpMethods.POST, entity = entity))
    } yield response
    fCheckResp.transform(Success(_))
  }

}
