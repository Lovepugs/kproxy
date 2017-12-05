package com.digischool.kkp.keycloak.utils

import com.digischool.kkp.core.models.config.{HostConfiguration, BackEnd, ServerDefinition}


object TestConfigurationRepository  {

  val kpublic = """{
                 |  "realm": "digischool",
                 |  "realm-public-key": "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAukquwrdjmAF6920E67OK1RLM58VCdEm7X12hkOYNj2VnoQwEZeO4L2uwdHKebsxPGZKoCHOqQ73UayacLS4lpnYJ5Hvtzmbmn7gIBhthr+s9J5PR13Vc8cYrO79dDYYtxQitVvpmMOO1prPzAr1Z591WBzAp6SJz9kJWNtjf85eBqS9mmscU3PuT+vkio8BfujgQitFwrLy9cc7vJQsEQ1dagk1ZYc5rsgeMtkNKErSlI+/afCXPlFcju+VPctR8m0zQR+qDKz6azlx+gdHVrQ9WJbpFPaZYf6QYH7OnU8TXGMaaJYASCHdlW5HQBhKpcb8pnzacpQ2X8iko5WRsJwIDAQAB",
                 |  "auth-server-url": "http://localhost:8080/auth",
                 |  "ssl-required": "external",
                 |  "resource": "cdr",
                 |  "public-client": true
                 |}""".stripMargin


  val kprivate = """{
                  |  "realm": "digischool",
                  |  "realm-public-key": "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAukquwrdjmAF6920E67OK1RLM58VCdEm7X12hkOYNj2VnoQwEZeO4L2uwdHKebsxPGZKoCHOqQ73UayacLS4lpnYJ5Hvtzmbmn7gIBhthr+s9J5PR13Vc8cYrO79dDYYtxQitVvpmMOO1prPzAr1Z591WBzAp6SJz9kJWNtjf85eBqS9mmscU3PuT+vkio8BfujgQitFwrLy9cc7vJQsEQ1dagk1ZYc5rsgeMtkNKErSlI+/afCXPlFcju+VPctR8m0zQR+qDKz6azlx+gdHVrQ9WJbpFPaZYf6QYH7OnU8TXGMaaJYASCHdlW5HQBhKpcb8pnzacpQ2X8iko5WRsJwIDAQAB",
                  |  "auth-server-url": "http://localhost:8080/auth",
                  |  "ssl-required": "external",
                  |  "resource": "cdr",
                  |  "credentials": {
                  |    "secret": "21ff46b0-4801-47b0-b362-b6b744e4338c"
                  |  }
                  |}""".stripMargin


  val routes1 =
    """GET|POST /                         Allow
      |GET      /toto                     Deny
      |GET      /@documentation/Home      Allow
    """.stripMargin

  val routes2 =
    """GET|POST /                         Logged
      |GET      /toto                     Allow
      |GET      /@documentation/Home      Deny
    """.stripMargin


  val configs = List(
    HostConfiguration(
      server = ServerDefinition(hosts = List("toto.lo"), port = 8181),
      routes  = Nil, topFilter = "", bottomFilter = ""),

    HostConfiguration(
      server = ServerDefinition(hosts = List("titi.lo"), port = 8181, rootPath = "titi"),
      routes  = Nil, topFilter = "", bottomFilter = "")
  )

}
