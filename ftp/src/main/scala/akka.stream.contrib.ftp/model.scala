/*
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com>
 */
package akka.stream.contrib.ftp

import java.net.InetAddress
import org.apache.commons.net.ftp.FTPFile
import scala.language.implicitConversions

final case class FtpFile(name: String)
object FtpFile {
  implicit def fromCommonsNetFtpFile(cnetFtpFile: FTPFile): FtpFile = {
    FtpFile(cnetFtpFile.getName)
  }
  implicit def fromArrayCommonsNetFtpFile(cnetFiles: Array[FTPFile]): Array[FtpFile] = {
    cnetFiles.map(fromCommonsNetFtpFile)
  }
}

sealed abstract class FtpConnectionSettings extends Product with Serializable {
  def host: InetAddress
  def port: Int
  def credentials: FtpCredentials
}
object FtpConnectionSettings {
  final val DefaultFtpPort = 21
  final case class BasicFtpConnectionSettings(
    host:        InetAddress,
    port:        Int,
    credentials: FtpCredentials
  ) extends FtpConnectionSettings
  final case class SshBasicFtpConnectionSettings(
    host:                  InetAddress,
    port:                  Int,
    credentials:           FtpCredentials,
    strictHostKeyChecking: Boolean        = true
  ) extends FtpConnectionSettings
}

sealed abstract class FtpCredentials extends Product with Serializable {
  def username: String
  def password: String
}
object FtpCredentials {
  final val Anonymous = "anonymous"
  case object AnonFtpCredentials extends FtpCredentials {
    val username = Anonymous
    val password = Anonymous
  }
  final case class NonAnonFtpCredentials(username: String, password: String)
    extends FtpCredentials
}
