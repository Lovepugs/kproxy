name := """core"""



libraryDependencies ++= Seq(
  "com.kreactive" %% "brigitte" % Version.briGitte,
  "com.typesafe.akka" %% "akka-stream" % Version.akka,
  "com.typesafe.akka" %% "akka-http" % Version.akkaHttp,
  "com.typesafe.play" %% "play-json" % Version.playJson,
  "com.github.pathikrit" %% "better-files" % Version.betterFiles,
  "org.eclipse.jgit" % "org.eclipse.jgit" % "4.7.1.201706071930-r",

  "com.typesafe.akka" %% "akka-http-testkit" % Version.akkaHttp % "test",
  "org.scalatest" %% "scalatest" % Version.scalaTest % "test"
)