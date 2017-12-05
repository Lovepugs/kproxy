package com.digischool.kkp.core.directives

import akka.http.scaladsl.server.{ Directive1, Directive0}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.{Credentials, BasicDirectives}

trait BasicHttpAuthDirective extends BasicDirectives {

  /**
   * BasicHttpAuth
   *
   * @param user : String the userID
   * @param password : String the user's password
   * @return
   */
  def basicHttpAuth(user:String, password:String): Directive1[String] = authenticateBasic(user, myUserPassAuthenticator(password))


  def basicHttpAuth0(user:String, password:String, realm: Option[String] = None): Directive0 =
    authenticateBasic(realm.getOrElse(user), myUserPassAuthenticator(password)).map{username => ()}


  def myUserPassAuthenticator(password:String)(credentials: Credentials): Option[String] =
    Some(credentials) collect {
      case p @ Credentials.Provided(id) if p.verify(password) => id
    }
}

object BasicHttpAuthDirective extends BasicHttpAuthDirective