useJGit

version in ThisBuild := "0.9.0"

scalaVersion in ThisBuild := Version.scalaVersion

organization in ThisBuild := "com.digischool.kproxy"

scalacOptions in ThisBuild := Seq("-unchecked", "-feature", "-deprecation", "-encoding", "utf8")

Revolver.settings

resolvers in ThisBuild += Resolver.bintrayRepo("kreactive", "maven")

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-slf4j" % Version.akka,
  "ch.qos.logback" % "logback-classic" % "1.1.3"
)

javaOptions in reStart += "-Djava.net.preferIPv4Stack=true"

lazy val outputmodel = project in file("modules/kkp.outputmodel")

lazy val akkaTokenExtractor =
  project in file("modules/kkp.akkatokenextractor") dependsOn outputmodel aggregate outputmodel

lazy val kkpdsls2 = project in file("modules/kkp.dsls2")

lazy val core = (project in file("modules/kkp.core")).
  enablePlugins(BuildInfoPlugin).
  settings(
    buildInfoKeys := Seq[BuildInfoKey](name in kproxy, version, scalaVersion, sbtVersion, git.gitHeadCommit),
    buildInfoPackage := "com.digischool.kkp.core"
  )
  .dependsOn(kkpdsls2).aggregate(kkpdsls2)

lazy val keycloak = (project in file("modules/kkp.keycloak"))
  .dependsOn(core, core % "test->test").aggregate(core)
  .dependsOn(outputmodel).aggregate(outputmodel)
  .dependsOn(akkaTokenExtractor).aggregate(akkaTokenExtractor)

lazy val commonfilters = (project in file("modules/kkp.commonfilters"))
  .dependsOn(core).aggregate(core)

lazy val kproxy: Project = (project in file("."))
  .dependsOn(core).aggregate(core)
  .dependsOn(keycloak).aggregate(keycloak)
  .dependsOn(commonfilters).aggregate(commonfilters)
  .enablePlugins(GitVersioning)
  .enablePlugins(JavaAppPackaging)
  .enablePlugins(SbtNativePackager)
  .enablePlugins(DockerPlugin)
  .enablePlugins(RpmPlugin)

lazy val publishLibs = TaskKey[Unit]("publishLibs", "publishes library modules to artifactory")

publishLibs := Seq(outputmodel, akkaTokenExtractor).map(publish in _).dependOn.value