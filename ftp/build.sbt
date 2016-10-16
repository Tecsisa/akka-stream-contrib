lazy val ftp = (project in file(".")).
  enablePlugins(AutomateHeaderPlugin)

name := "akka-stream-contrib-ftp"

libraryDependencies ++= Seq(
  "commons-net" % "commons-net" % "3.5", // ALv2
  "com.jcraft" % "jsch" % "0.1.54", // BSD
  "org.apache.ftpserver" % "ftpserver-core" % "1.0.6" % "test", // ALv2
  "org.apache.sshd" % "sshd-core" % "1.3.0" % "test", // ALv2
  "org.slf4j" % "slf4j-api" % "1.7.21" % "test", // MIT
  "ch.qos.logback" % "logback-classic" % "1.1.7" % "test", // EPL v1.0
  "ch.qos.logback" % "logback-core" % "1.1.7" % "test" // EPL v1.0
)