lazy val ftp = (project in file(".")).
  enablePlugins(AutomateHeaderPlugin)

name := "akka-stream-contrib-ftp"

libraryDependencies ++= Seq(
  "commons-net" % "commons-net" % "3.5", // ALv2
  "com.jcraft" % "jsch" % "0.1.54", // BSD
  "org.apache.ftpserver" % "ftpserver-core" % "1.0.6" % "test", // ALv2
  "org.slf4j" % "slf4j-log4j12" % "1.7.21" % "test" // MIT
)
