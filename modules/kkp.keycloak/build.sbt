name := """keycloak"""



libraryDependencies ++= Seq(
  "com.kreactive" %% "keycloak-authenticator" % Version.keycloak,

  "com.typesafe.akka" %% "akka-testkit" % Version.akka % "test",
  "com.typesafe.akka" %% "akka-http-testkit" % Version.akkaHttp % "test",
  "org.scalatest" %% "scalatest" % Version.scalaTest % "test")