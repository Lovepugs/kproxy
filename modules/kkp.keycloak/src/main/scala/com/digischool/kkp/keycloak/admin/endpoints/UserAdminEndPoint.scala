package com.digischool.kkp.keycloak.admin.endpoints

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.directives.HostDirectives
import com.digischool.kkp.core.directives.{BasicHttpAuthDirective, HeaderDirective, OriginalAuthorityDirective, PortDirective}
import com.digischool.kkp.core.injectable._
import com.digischool.kkp.core.utils.PlayJsonSupport._
import com.digischool.kkp.core.utils.cors.CorsUtils
import com.digischool.kkp.keycloak.admin.models.{SocialCreate, UserPassCreate, UserPassReset}
import com.digischool.kkp.keycloak.models.RoleCommand
import com.digischool.kkp.keycloak.services.UserAdmin
import com.kreactive.model.UserId
import org.keycloak.adapters.KeycloakDeployment
import play.api.libs.json.Json

import scala.util.{Failure, Success}

trait UserAdminEndPoint extends
  BasicHttpAuthDirective with
  HeaderDirective with
  AdminEndPointsAction with
  OriginalAuthorityDirective with
  HostDirectives with
  PortDirective with
  CorsUtils with
  UserAdmin {
  self: WithNameableLog with WithExecutionContext with WithHttpExt with WithMaterializer =>

  private lazy val log = logAs(getClass)

  /**
    *
    * @api {post} /admin/user/{{userId}}/roles Set or unset roles for a user on a given Keycloak client
    * @apiName Roles
    * @apiGroup Keycloak Admin
    * @apiHeader {basic} Authorization a basic authentication hash with username {{realm}} and correct password
    * @apiHeaderExample {String} Basic auth
    *                  Authorization: Basic ZGlnaVNjaG9vbDpwYXNzd29yZA==
    * @apiParam {get} userId the UUID identifying the user for whom we want to update the roles
    * @apiParam {string} clientName the client identifier on which to update the roles. Use "realm" for realm-level roles.
    * @apiParam {String[]} [add] the <code>roles</code> we wish to add.
    *          If the given roles do not exist in the client, they will be silently ignored.
    * @apiParam {String[]} [remove] the <code>roles</code> we wish to remove.
    *          If the given roles do not exist in the client or are not yet assigned to the user, they will be silently ignored.
    * @apiSuccessExample {string} Success-Response:
    *                   The result was ()
    * @apiError {string} BadRequest (400) An error occurred: [error message]
    */
  def roles(deployment:KeycloakDeployment, password:String): Route = post{
    path(ROLE) { (userId: UserId) =>
      basicHttpAuth(deployment.getRealm, password) { _ =>
        flagDrainedEntity(as[RoleCommand]) { (roleCommand: RoleCommand) =>
          onComplete(addOrRemoveRoles(userId, roleCommand, deployment)) {
            case Success(value) => complete(s"The result was $value")
            case Failure(ex) => complete(StatusCodes.BadRequest, s"An error occurred: ${ex.getMessage}")
          }
        }
      }
    }
  }


  def create(deployment:KeycloakDeployment, password:String): Route = cors() {
    post{
      path(CREATE_USER_PASS) {
        flagDrainedEntity(as[UserPassCreate]) { (u: UserPassCreate) =>
          onComplete(createMobile(u,deployment)) {
            case Success(userId) => complete(StatusCodes.Created, Json.obj("id" -> userId))
            case Failure(x) => complete(StatusCodes.BadRequest, Json.obj("error" -> s"An error occurred: ${x.getMessage}"))
          }
        }
      }
    }
  }

  def createSocial(deployment:KeycloakDeployment, password:String): Route = post{
    path(CREATE_SOCIAL) {
      flagDrainedEntity(as[SocialCreate]) { (u: SocialCreate) =>
        onComplete(createMobileSocial(u,deployment)) {
          case Success(userId) => complete(StatusCodes.Created, Json.obj("id" -> userId))
          case Failure(x) => complete(StatusCodes.BadRequest, Json.obj("error" -> s"An error occurred: ${x.getMessage}"))
        }
      }
    }
  }

  val extractOrignalAuthorityCompounds: Directive[(String, String,Int)] = extractOriginalScheme & extractHost & extractOriginalPort

  def resetPassword(deployment:KeycloakDeployment, profileUrl:String): Route = cors() {
    post{
      path(RESET_USER_PASS) {
        flagDrainedEntity(as[UserPassReset]) { (u: UserPassReset) =>
          extractOrignalAuthorityCompounds { case (scheme, host, port) =>
            onComplete(resetPasswordByMail(u, deployment, profileUrl, scheme, host, port)) {
              case Success(x) =>
                log.error("Step5 REPONSE " + x)
                complete(StatusCodes.OK)
              case Failure(e) => complete(StatusCodes.BadRequest, Json.obj("error" -> s"An error occurred: ${e.getClass.getSimpleName} -- ${e.getMessage}"))
            }
          }
        }
      }
    }
  }

  def redirectAccount(deployment:KeycloakDeployment, profileUrl:String): Route = post{
    pathPrefix("auth" / "realms" / s"${deployment.getRealm}" / "account") {
      drainRequest & redirect(profileUrl, StatusCodes.MovedPermanently)
    }
  }

  def user(deployment: KeycloakDeployment, password: String): Route = get{
    path(GET_USER) { userId =>
      basicHttpAuth(deployment.getRealm, password) { userName =>
        (drainRequest & onComplete(getUserJson(userId, deployment))) {
          case Success(userJson) => complete(StatusCodes.OK, userJson)
          case Failure(x) => complete(StatusCodes.BadRequest, Json.obj("error" -> s"An error occurred: ${x.getMessage}"))
        }
      }
    }
  }

  def locale(locales:List[String]): Route = get {
    path(VALID_LOCALES) {
      drainRequest & complete(StatusCodes.OK, Json.obj("langs" -> locales))
    }
  }
}
