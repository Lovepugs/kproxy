import com.typesafe.sbt.packager.MappingsHelper
import com.typesafe.sbt.packager.docker.Cmd

mappings in Universal ++= MappingsHelper.directory(baseDirectory.value / "dockers" / "kproxy")

mappings in Universal ++= MappingsHelper.directory(baseDirectory.value / "src" / "main" / "resources")

//version in Docker := "latest"

packageName in Docker := "fmasion/kproxy"

dockerUpdateLatest := true

dockerExposedVolumes  += "/opt/docker/conf"

maintainer in Docker := "fmasion"

dockerExposedPorts := Seq(8181)

dockerCommands += Cmd("CMD", "-Dconfig.file=resources/application.docker.conf")