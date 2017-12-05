package com.digischool.kkp.core.configrepository.impl

import akka.stream.scaladsl.Source
import com.digischool.kkp.core.configrepository.ConfigRepository
import com.digischool.kkp.core.configrepository.model.ModularConfig
import com.digischool.kkp.core.models.config.{HostConfiguration, ServerDefinition}

object TestConfigRepository extends ConfigRepository {

  val kpublic =
    """{
      |  "realm": "digischool",
      |  "realm-public-key": "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAukquwrdjmAF6920E67OK1RLM58VCdEm7X12hkOYNj2VnoQwEZeO4L2uwdHKebsxPGZKoCHOqQ73UayacLS4lpnYJ5Hvtzmbmn7gIBhthr+s9J5PR13Vc8cYrO79dDYYtxQitVvpmMOO1prPzAr1Z591WBzAp6SJz9kJWNtjf85eBqS9mmscU3PuT+vkio8BfujgQitFwrLy9cc7vJQsEQ1dagk1ZYc5rsgeMtkNKErSlI+/afCXPlFcju+VPctR8m0zQR+qDKz6azlx+gdHVrQ9WJbpFPaZYf6QYH7OnU8TXGMaaJYASCHdlW5HQBhKpcb8pnzacpQ2X8iko5WRsJwIDAQAB",
      |  "auth-server-url": "http://localhost:8080/auth",
      |  "ssl-required": "external",
      |  "resource": "cdr",
      |  "public-client": true
      |}""".
      stripMargin


  val kprivate =
    """{
      |  "realm": "digischool",
      |  "realm-public-key": "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAukquwrdjmAF6920E67OK1RLM58VCdEm7X12hkOYNj2VnoQwEZeO4L2uwdHKebsxPGZKoCHOqQ73UayacLS4lpnYJ5Hvtzmbmn7gIBhthr+s9J5PR13Vc8cYrO79dDYYtxQitVvpmMOO1prPzAr1Z591WBzAp6SJz9kJWNtjf85eBqS9mmscU3PuT+vkio8BfujgQitFwrLy9cc7vJQsEQ1dagk1ZYc5rsgeMtkNKErSlI+/afCXPlFcju+VPctR8m0zQR+qDKz6azlx+gdHVrQ9WJbpFPaZYf6QYH7OnU8TXGMaaJYASCHdlW5HQBhKpcb8pnzacpQ2X8iko5WRsJwIDAQAB",
      |  "auth-server-url": "http://localhost:8080/auth",
      |  "ssl-required": "external",
      |  "resource": "cdr",
      |  "public-client": true
      |}""".stripMargin


  val routes1 =
    """GET|POST /<END>                    Logged
      |GET      /toto<END>                Deny
      |GET      /assets                   Allow
      |GET      /@documentation/Home      Allow
      |GET      /@documentation           Allow
    """.stripMargin

  val routes2 =
    """GET|POST /<END>                    Allow
      |GET      /toto<END>                Logged
      |GET      /titi<END>                Logged
      |GET      /tata<END>                Logged
      |GET      /@documentation/Home      Deny
      |GET      /@documentation           Allow
      |GET      /assets                   Allow
    """.stripMargin


  val hc1 = new HostConfiguration(server = ServerDefinition(hosts = List("toto.lo"), port = 8181),
    routes = Nil, topFilter = "", bottomFilter = "")

  val hc2 = HostConfiguration(
    server = ServerDefinition(hosts = List("titi.lo"), port = 8181),
    routes = Nil, topFilter = "", bottomFilter = "")

  val configs = List(hc1, hc2)

  def getModules: List[String] = Nil

  override def source = Source.single(ModularConfig(Map(), Nil))
}
