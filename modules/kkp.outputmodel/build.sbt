name := """outputmodel"""

libraryDependencies ++= Seq(
  "com.kreactive" %% "basic-model" % Version.basicModel,
  "com.kreactive" %% "capsule-corp" % Version.capsuleCorp,
  "org.scalatest" %% "scalatest" % Version.scalaTest % "test"
)