rpmVendor := "kreactive"

linuxPackageMappings in Rpm := linuxPackageMappings.value

packageSummary in Linux := "KProxy the wonderfull and magical"

packageDescription := "Kproxy the proxy server with ponies inside"

rpmRelease := "1"

rpmLicense := Some("APL v2")

rpmBrpJavaRepackJars := true

rpmGroup := Some("MyGroup")

