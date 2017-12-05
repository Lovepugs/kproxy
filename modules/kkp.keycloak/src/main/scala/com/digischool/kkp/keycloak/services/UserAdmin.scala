package com.digischool.kkp.keycloak.services

import java.util.NoSuchElementException

import akka.actor.{ExtendedActorSystem, Extension, ExtensionId}
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.model.{FormData, _}
import com.digischool.kkp.core.injectable._
import com.digischool.kkp.core.utils.HttpUtils
import com.digischool.kkp.core.utils.PlayJsonSupport._
import com.digischool.kkp.keycloak._
import com.digischool.kkp.keycloak.admin.endpoints.ResetPasswordFlow
import com.digischool.kkp.keycloak.admin.models.{SocialCreate, UserPassCreate, UserPassReset}
import com.digischool.kkp.keycloak.models._
import com.digischool.kkp.keycloak.models.headers.RolesHeader
import com.kreactive.model.{ApplicationId, Role, UserId}
import org.keycloak.adapters.KeycloakDeployment
import play.api.libs.json.{JsObject, JsValue}

import scala.collection.immutable.{Seq => ISeq}
import scala.concurrent.Future

trait UserAdmin extends Extension with TokenHandler with HttpUtils with ResetPasswordFlow {
  self: WithNameableLog with WithExecutionContext with WithHttpExt with WithMaterializer =>
  lazy val userAdminLog = logAs(getClass)

  def getToken(deployment: KeycloakDeployment, tokenO: Option[String] = None): Future[String] = {
    tokenO.map(Future.successful).getOrElse {
      userAdminLog.debug("RETRIEVING TOKEN")
      clientAccess(deployment.clientsecret)(deployment).map(_.getToken).recover {
        case err =>
          userAdminLog.error(err, "UNABLE TO RETREIVE TOKEN")
          throw err
      }
    }
  }

  def createAndResetPassword(user: UserRepresentation, deployment: KeycloakDeployment): Future[UserId] = {
    for {
      token <- getToken(deployment)
      Some(userId) <- create(user, deployment)
      addedClient <- addMigratedClientRoles(deployment, token, user, userId)
      addedRealm <- addMigratedRealmRoles(deployment, token, user, userId)
      _ <- resetPassword(userId, user, deployment)
    } yield {
      userId
    }
  }

  def createAndAddProvider(user: UserRepresentation, deployment: KeycloakDeployment): Future[UserId] = {
    for {
      token <- getToken(deployment)
      Some(userId) <- create(user, deployment)
      addedClient <- addMigratedClientRoles(deployment, token, user, userId)
      addedRealm <- addMigratedRealmRoles(deployment, token, user, userId)
      _ <- addProvider(userId, user, deployment)
    } yield {
      userId
    }
  }

  def resetPasswordByMail(user: UserPassReset, deployment: KeycloakDeployment, profileUrl: String, scheme: String, host: String, port: Int): Future[HttpResponse] = {
    for {
      entity <- Marshal(FormData(Map("username" -> user.mail))).to[RequestEntity]
      (url, cookie) <- resetPassUrl(deployment, profileUrl)
      response1 <- requestAndDrain(HttpRequest(uri = url, method = HttpMethods.POST, entity = entity, headers = cookie :: Nil))
      location = response1.header[Location].get.uri
      response2 <- requestAndDrain(HttpRequest(uri = location, method = HttpMethods.GET))
    } yield {
      userAdminLog.info("Step4 URI " + url)
      response2
    }
  }

  def create(u: UserRepresentation, d: KeycloakDeployment): Future[Option[UserId]] = {
    val createUri = s"${d.getAuthServerBaseUrl}/admin/realms/${d.getRealm}/users"
    val fEntity: Future[MessageEntity] = Marshal(u).to[RequestEntity]
    val fHeaders = getAuthorizationHeaders(d)

    for {
      entity <- fEntity
      headers <- fHeaders
      createResponse <- requestAndDrain(HttpRequest(uri = createUri, method = HttpMethods.POST, entity = entity, headers = headers))
    } yield {
      getUserId(createResponse)
    }
  }

  def createMobile(u: UserPassCreate, d: KeycloakDeployment): Future[UserId] = {
    val userRepresentation: UserRepresentation = UserRepresentation.fromUserPassCreate(u)
    createAndResetPassword(userRepresentation, d)
  }

  def createMobileSocial(u: SocialCreate, d: KeycloakDeployment): Future[UserId] = {
    val userRepresentation: UserRepresentation = UserRepresentation.fromSocialCreate(u)
    createAndAddProvider(userRepresentation, d)
  }

  def resetPassword(uId: UserId, u: UserRepresentation, d: KeycloakDeployment): Future[Unit] = {
    //PUT /admin/realms/{realm}/users/{id}/reset-password
    val resetUri = s"${d.getAuthServerBaseUrl}/admin/realms/${d.getRealm}/users/$uId/reset-password"
    val credentialRepresentation: CredentialRepresentation = u.credentials.toList.flatten.head
    val fCredentialEntity = Marshal(credentialRepresentation).to[RequestEntity]
    val fHeaders = getAuthorizationHeaders(d)

    for {
      credentialEntity <- fCredentialEntity
      headers <- fHeaders
      _ <- requestAndDrain(HttpRequest(uri = resetUri, method = HttpMethods.PUT, entity = credentialEntity, headers = headers))
    } yield ()
  }

  def addProvider(uId: UserId, u: UserRepresentation, d: KeycloakDeployment): Future[Unit] = {
    //POST /admin/realms/{realm}/users/{userId}/federated-identity/{provider}
    def addUri(provider: String) = s"${d.getAuthServerBaseUrl}/admin/realms/${d.getRealm}/users/$uId/federated-identity/$provider"

    val federated: FederatedIdentityRepresentation = u.federatedIdentities.toList.flatten.head
    val fFederatedEntity = Marshal(federated).to[RequestEntity]
    val fHeaders = getAuthorizationHeaders(d)

    for {
      credentialEntity <- fFederatedEntity
      headers <- fHeaders
      _ <- requestAndDrain(HttpRequest(uri = addUri(federated.identityProvider), method = HttpMethods.POST, entity = credentialEntity, headers = headers))
    } yield ()
  }

  def getUserJson(userId: UserId, deployment: KeycloakDeployment): Future[JsValue] = {
    val userUri = s"${deployment.getAuthServerBaseUrl}/admin/realms/${deployment.getRealm}/users/$userId"
    for {
      headers <- getAuthorizationHeaders(deployment)
      userJson <- requestAndUnmarshalOK[JsObject](HttpRequest(uri = userUri, method = HttpMethods.GET, headers = headers))
    } yield userJson
  }

  private def getAuthorizationHeaders(deployment: KeycloakDeployment): Future[ISeq[HttpHeader]] = {
    getToken(deployment).map { t =>
      ISeq(Authorization(OAuth2BearerToken(t)))
    }
  }

  private def addMigratedClientRoles(deployment: KeycloakDeployment, token: String, user: UserRepresentation, userId: UserId): Future[Any] = {
    user.clientRoles.map { (rolemap: Map[ApplicationId, Set[Role]]) =>
      if (rolemap.isEmpty) Future.successful(Unit)
      else Future.traverse(roles2RoleCommands(rolemap)) {
        case RoleCommand(_, Nil, _) => Future.successful(Unit)
        case RoleCommand(clientName, add, _) => addClientRoles(deployment, userId, clientName, add, token)
      }
    }.getOrElse(Future.successful(Unit))
  }

  private def addMigratedRealmRoles(deployment: KeycloakDeployment, token: String, user: UserRepresentation, userId: UserId): Future[Any] = {
    user.realmRoles.map { (roleset: Set[Role]) =>
      roleset.toList match {
        case Nil => Future.successful(Unit)
        case _ =>
          Future.traverse(roles2RoleCommands(Map(RolesHeader.REALM -> roleset))) {
            case RoleCommand(_, Nil, _) => Future.successful(Unit)
            case RoleCommand(_, add, _) => addRealmRoles(deployment, userId, add, token)
          }
      }
    }.getOrElse(Future.successful(Unit))
  }

  private def roles2RoleCommands(roleMap: Map[ApplicationId, Set[Role]]): List[RoleCommand] = {
    roleMap.map { case (clientName: ApplicationId, roles: Set[Role]) if roles.nonEmpty =>
      RoleCommand(clientName = clientName, add = roles.toList, remove = Nil)
    }.toList
  }


  def updateUserPassword(username: String, password: String, deployment: KeycloakDeployment): Future[UserId] = {
    //PUT /admin/realms/{realm}/users/{id}/reset-password
    def credentialsUri(id: UserId) = s"${deployment.getAuthServerBaseUrl}/admin/realms/${deployment.getRealm}/users/$id/reset-password"

    val credential = CredentialRepresentation(`type` = "password", value = password, temporary = false)
    val fCredentialEntity = Marshal(credential).to[RequestEntity]
    val fHeaders = getAuthorizationHeaders(deployment)

    (for {
      headers <- fHeaders
      Some(user) <- findUser(username, deployment)
      Some(userId) = user.id
      credentialEntity <- fCredentialEntity
      _ <- requestAndDrain(HttpRequest(uri = credentialsUri(userId), method = HttpMethods.PUT, entity = credentialEntity, headers = headers))
    } yield userId).recover{
      case nsee: NoSuchElementException =>
        userAdminLog.error(s"found no user with username $username", nsee)
        throw new Exception(s"User not found: $username")
    }
  }

  def findUser(username: String, deployment: KeycloakDeployment): Future[Option[UserRepresentation]] = {
    val realm = deployment.getRealm
    val findUser = s"${deployment.getAuthServerBaseUrl}/admin/realms/$realm/users?username=$username"

    for {
      token <- getToken(deployment)
      headers: ISeq[HttpHeader] = ISeq(Authorization(OAuth2BearerToken(token)))
      response <- requestAndUnmarshalOK[List[UserRepresentation]](HttpRequest(uri = findUser, method = HttpMethods.GET, headers = headers))
    } yield response.headOption
  }

  def addOrRemoveRoles(userId: UserId, roleCommand: RoleCommand, deployment: KeycloakDeployment): Future[Unit] = {
    //log.error("DEPLOY : " + deployment.getAuthUrl)
    if (roleCommand.isRealmCommand) {
      for {
        token <- getToken(deployment)
        added <- if (roleCommand.add.nonEmpty) addRealmRoles(deployment, userId, roleCommand.add, token) else Future.successful(Unit)
        removed <- if (roleCommand.remove.nonEmpty) removeRealmRoles(deployment, userId, roleCommand.remove, token) else Future.successful(Unit)
      } yield {
        ()
      }
    } else {
      val clientName = roleCommand.clientName
      for {
        token <- getToken(deployment)
        added <- if (roleCommand.add.nonEmpty) addClientRoles(deployment, userId, clientName, roleCommand.add, token) else Future.successful(Unit)
        removed <- if (roleCommand.remove.nonEmpty) removeClientRoles(deployment, userId, clientName, roleCommand.remove, token) else Future.successful(Unit)
      } yield {
        ()
      }
    }
  }


  private def addClientRoles(deployment: KeycloakDeployment, userId: UserId, clientName: ApplicationId, roles: List[Role], token: String): Future[Unit] = {
    for {
      clientIdO <- getIdByClientId(clientName, deployment, Some(token)) if clientIdO.isDefined
      availableRoles <- getAvailableRoles(userId, clientIdO, deployment, Some(token))
      tobeAdded: List[RoleRepresentation] = availableRoles.filter(ar => roles.map(_.name).exists(ar.name.name.equalsIgnoreCase))
      addedClientRoles <- addRoles(userId, clientIdO, deployment, tobeAdded, Some(token))
    } yield {
      ()
    }
  }

  private def addRealmRoles(deployment: KeycloakDeployment, userId: UserId, roles: List[Role], token: String): Future[Unit] = {
    for {
      availableRoles <- getAvailableRoles(userId, None, deployment, Some(token))
      tobeAdded: List[RoleRepresentation] = {
        val addIt = availableRoles.filter(ar => roles.map(_.name).exists(ar.name.name.equalsIgnoreCase))
        userAdminLog.debug(s"Roles to ADD : $addIt")
        addIt
      }
      addedClientRoles <- addRoles(userId, None, deployment, tobeAdded, Some(token))
    } yield {
      ()
    }
  }

  private def removeClientRoles(deployment: KeycloakDeployment, userId: UserId, clientName: ApplicationId, roles: List[Role], token: String): Future[Unit] = {
    for {
      clientIdO <- getIdByClientId(clientName, deployment, Some(token)) if clientIdO.isDefined
      activesRoles <- getActiveRoles(userId, clientIdO, deployment, Some(token))
      tobeRemoved: List[RoleRepresentation] = {
        val removeIt = activesRoles.filter(ar => roles.map(_.name).exists(ar.name.name.equalsIgnoreCase))
        userAdminLog.debug(s"Roles to ADD : $removeIt")
        removeIt
      }
      addedClientRoles <- removeRoles(userId, clientIdO, deployment, tobeRemoved, Some(token))
    } yield {
      ()
    }
  }

  private def removeRealmRoles(deployment: KeycloakDeployment, userId: UserId, roles: List[Role], token: String): Future[Unit] = {
    for {
      activesRoles <- getActiveRoles(userId, None, deployment, Some(token))
      tobeRemoved: List[RoleRepresentation] = {
        val removeIt = activesRoles.filter(ar => roles.map(_.name).exists(ar.name.name.equalsIgnoreCase))
        userAdminLog.debug(s"Roles to ADD : $removeIt")
        removeIt
      }
      addedClientRoles <- removeRoles(userId, None, deployment, tobeRemoved, Some(token))
    } yield {
      ()
    }
  }

  private def getClients(deployment: KeycloakDeployment, tokenO: Option[String] = None): Future[List[ClientRepresentationLight]] = {
    val clientUrl = s"${deployment.getAuthServerBaseUrl}/admin/realms/${deployment.getRealm}/clients"
    for {
      token <- getToken(deployment, tokenO)
      headers: ISeq[HttpHeader] = ISeq(Authorization(OAuth2BearerToken(token)))
      clients <- requestAndUnmarshalOK[List[ClientRepresentationLight]](
        HttpRequest(uri = clientUrl, method = HttpMethods.GET, headers = headers)
      ).recover {
        case _ =>
          userAdminLog.error("TOKEN HAS NO RIGHT TO MANAGE USER")
          Nil
      }
    } yield clients

  }

  private def getClientByClientId(clientId: ApplicationId, deployment: KeycloakDeployment, tokenO: Option[String] = None): Future[Option[ClientRepresentationLight]] = {
    getClients(deployment, tokenO).map {
      _.find(r => r.clientId.value.equalsIgnoreCase(clientId.value))
    }.recover {
      case e => userAdminLog.error("Could not access client " + e); throw e
    }
  }

  private def getIdByClientId(clientId: ApplicationId, deployment: KeycloakDeployment, tokenO: Option[String] = None): Future[Option[ClientId]] = {
    getClientByClientId(clientId, deployment, tokenO).map {
      _.map(_.id)
    }.recover {
      case e => userAdminLog.error("Could not access client " + e); throw e
    }
  }


  private def getAvailableRoles(userId: UserId, clientId: Option[ClientId], deployment: KeycloakDeployment, tokenO: Option[String] = None): Future[List[RoleRepresentation]] = {
    val availableRolesURL = clientId.fold {
      s"${deployment.getAuthServerBaseUrl}/admin/realms/${deployment.getRealm}/users/$userId/role-mappings/realm/available"
    } { cId =>
      s"${deployment.getAuthServerBaseUrl}/admin/realms/${deployment.getRealm}/users/$userId/role-mappings/clients/$cId/available"
    }
    for {
      token <- getToken(deployment, tokenO)
      headers: ISeq[HttpHeader] = ISeq(Authorization(OAuth2BearerToken(token)))
      _ = userAdminLog.debug(s"CALLING AVAILABLE ROLES $availableRolesURL")
      roles <- requestAndUnmarshalOK[List[RoleRepresentation]](HttpRequest(uri = availableRolesURL, method = HttpMethods.GET, headers = headers))
    } yield {
      userAdminLog.debug("ROLES AVAILABLE : " + roles)
      roles
    }
  }


  private def getActiveRoles(userId: UserId, clientId: Option[ClientId], deployment: KeycloakDeployment, tokenO: Option[String] = None): Future[List[RoleRepresentation]] = {
    val activesRolesURL = clientId.fold {
      s"${deployment.getAuthServerBaseUrl}/admin/realms/${deployment.getRealm}/users/$userId/role-mappings/realm"
    } { cId =>
      s"${deployment.getAuthServerBaseUrl}/admin/realms/${deployment.getRealm}/users/$userId/role-mappings/clients/$cId"
    }
    for {
      token <- getToken(deployment, tokenO)
      headers: ISeq[HttpHeader] = ISeq(Authorization(OAuth2BearerToken(token)))
      roles <- requestAndUnmarshalOK[List[RoleRepresentation]](HttpRequest(uri = activesRolesURL, method = HttpMethods.GET, headers = headers))
    } yield {
      userAdminLog.debug("ROLES ACTIVES : " + roles)
      roles
    }
  }

  private def addRoles(userId: UserId, clientId: Option[ClientId], deployment: KeycloakDeployment, roles: List[RoleRepresentation], tokenO: Option[String] = None): Future[Boolean] = {
    import com.digischool.kkp.core.utils.PlayJsonSupport._
    val postRolesURL = clientId.fold {
      s"${deployment.getAuthServerBaseUrl}/admin/realms/${deployment.getRealm}/users/$userId/role-mappings/realm"
    } { cId =>
      s"${deployment.getAuthServerBaseUrl}/admin/realms/${deployment.getRealm}/users/$userId/role-mappings/clients/$cId"
    }
    if (roles.nonEmpty) {
      for {
        token <- getToken(deployment, tokenO)
        myHeaders: ISeq[HttpHeader] = ISeq(Authorization(OAuth2BearerToken(token)))
        entity <- Marshal(roles).to[RequestEntity]
        _ = userAdminLog.debug(s"CALLING ADD ROLES $postRolesURL")
        response <- requestAndDrain(HttpRequest(uri = postRolesURL, method = HttpMethods.POST, headers = myHeaders, entity = entity))
      } yield {
        userAdminLog.debug("CALLING ADD ROLES FINISHED SUCCESSFULLY")
        response.status == StatusCodes.NoContent
      }
    } else {
      userAdminLog.debug("NO ROLES TO BE ADDED")
      Future.successful(true)
    }

  }

  private def removeRoles(userId: UserId, clientId: Option[ClientId], deployment: KeycloakDeployment, roles: List[RoleRepresentation], tokenO: Option[String] = None): Future[Boolean] = {
    import com.digischool.kkp.core.utils.PlayJsonSupport._
    val deleteRolesURL = clientId.fold {
      s"${deployment.getAuthServerBaseUrl}/admin/realms/${deployment.getRealm}/users/$userId/role-mappings/realm"
    } { cId =>
      s"${deployment.getAuthServerBaseUrl}/admin/realms/${deployment.getRealm}/users/$userId/role-mappings/clients/$cId"
    }
    if (roles.nonEmpty) {
      for {
        token <- getToken(deployment, tokenO)
        myHeaders: ISeq[HttpHeader] = ISeq(Authorization(OAuth2BearerToken(token)))
        entity <- Marshal(roles).to[RequestEntity]
        _ = userAdminLog.debug(s"CALLING REMOVE ROLES $deleteRolesURL")
        response <- requestAndDrain(HttpRequest(uri = deleteRolesURL, method = HttpMethods.DELETE, headers = myHeaders, entity = entity))
      } yield {
        userAdminLog.debug("CALLING REMOVE ROLES FINISHED SUCCESSFULLY")
        response.status == StatusCodes.NoContent
      }
    } else {
      userAdminLog.debug("NO ROLES TO BE REMOVED")
      Future.successful(true)
    }
  }


  private def getUserId(response: HttpResponse): Option[UserId] = {
    if (response.status == StatusCodes.Created) {
      val maybeHeader = response.headers.find {
        h => h.name().equalsIgnoreCase("Location")
      }
      maybeHeader.map { location =>
        val userId = location.value().split("/").reverse.head
        userAdminLog.info("CREATED USER ID " + userId)
        UserId.unapply(userId)
      }.getOrElse(None)
    } else {
      userAdminLog.error("UNABLE TO CREATE USER " + response.status)
      None
    }
  }

}

object UserAdmin extends ExtensionId[UserAdmin] {
  override def createExtension(sys: ExtendedActorSystem): UserAdmin = new UserAdmin with WithSystem {
    val system = sys
  }
}