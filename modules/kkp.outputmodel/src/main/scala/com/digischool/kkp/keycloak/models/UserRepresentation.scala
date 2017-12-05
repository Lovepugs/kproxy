package com.digischool.kkp.keycloak.models

import com.digischool.kkp.keycloak.admin.models.{SocialCreate, UserPassCreate}
import com.kreactive.model.{ApplicationId, Role, UserId}
import com.kreactive.util.MapUtils
import play.api.libs.json.Json

//if ever used replace Set[String] by Set[RequiredAction]
//sealed trait RequiredAction

case class CredentialRepresentation(`type`: String, value: String, temporary: Boolean = false)

case class FederatedIdentityRepresentation(identityProvider: String,
                                           userId: String,
                                           userName: String)

case class UserRepresentation(
                               id: Option[UserId],
                               username: String,
                               firstName: String,
                               lastName: String,
                               email: String,
                               attributes: Map[String, Set[String]] = Map.empty,
                               enabled: Boolean = true,
                               emailVerified: Boolean = false,
                               federatedIdentities: Option[List[FederatedIdentityRepresentation]] = None,
                               credentials: Option[List[CredentialRepresentation]] = None,
                               requiredActions: Set[String] = Set.empty,
                               realmRoles: Option[Set[Role]] = None,
                               clientRoles: Option[Map[ApplicationId, Set[Role]]] = None
                             ) {

  def withLocale(locale: String): UserRepresentation = {
    val newAttributes = attributes + ("locale" -> Set(locale))
    this.copy(attributes = newAttributes)
  }

  def withPassword(password: String): UserRepresentation = {
    val credential = CredentialRepresentation(`type` = "password", value = password, temporary = false)
    this.copy(credentials = Some(List(credential)))
  }

  def withSocialConnect(provider: String, providerUserId: String) = {
    val providedIdentity = FederatedIdentityRepresentation(provider, providerUserId, email)
    copy(federatedIdentities = Some(List(providedIdentity)))
  }

  def withRealmRole(role: Role): UserRepresentation = {
    val newRoles = realmRoles.getOrElse(Set.empty) + role
    this.copy(realmRoles = Some(newRoles))
  }

  def withClientRole(clientId: ApplicationId, role: Role): UserRepresentation = {
    val currentMap = clientRoles.getOrElse(Map.empty)
    val currentClientRoles = currentMap.getOrElse(clientId, Set.empty)
    val newClientRoles = currentClientRoles + role
    val newRoleMap = currentMap + (clientId -> newClientRoles)
    this.copy(clientRoles = Some(newRoleMap))
  }

}

object CredentialRepresentation {
  implicit val credentialRepresentationFormat = Json.format[CredentialRepresentation]
}

object FederatedIdentityRepresentation {
  implicit val federatedIdentityRepresentationFormat = Json.format[FederatedIdentityRepresentation]
}


object UserRepresentation extends MapUtils {
  implicit val rolesFormat = mapFormat[ApplicationId, Set[Role]]
  implicit val userRepresentationFormat = Json.format[UserRepresentation]

  def fromUserPassCreate(userpass: UserPassCreate): UserRepresentation = {
    UserRepresentation(id = None,
      username = userpass.username.getOrElse(userpass.mail),
      firstName = userpass.firstName.getOrElse(""),
      lastName = userpass.lastName.getOrElse(""),
      email = userpass.mail,
      enabled = true,
      emailVerified = false
    ).withLocale(userpass.locale)
      .withPassword(userpass.password)
  }

  def fromSocialCreate(social: SocialCreate): UserRepresentation = {
    UserRepresentation(id = None,
      username = social.username.getOrElse(social.mail),
      firstName = "",
      lastName = "",
      email = social.mail,
      enabled = true,
      emailVerified = false
    ).withLocale(social.locale)
      .withSocialConnect(social.provider, social.providerUserId)
  }

}
