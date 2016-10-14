/*
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com>
 */
package akka.stream.contrib.ftp

import org.apache.commons.net.ftp.{ FTPClient, FTPListParseEngine }
import scala.collection.immutable

trait Ftp {
  _: FtpLike[FTPClient] =>

  def connect(
    connectionSettings: FtpConnectionSettings
  )(implicit ftpClient: FTPClient): Unit = {
    if (ftpClient != null) {
      ftpClient.connect(connectionSettings.host, connectionSettings.port)
      ftpClient.login(
        connectionSettings.credentials.username,
        connectionSettings.credentials.password
      )
      ftpClient.enterLocalPassiveMode()
    }
  }

  def disconnect()(implicit ftpClient: FTPClient): Unit = {
    if (ftpClient != null) {
      if (ftpClient.isConnected) {
        ftpClient.logout()
        ftpClient.disconnect()
      }
    }
  }

  def listFiles()(implicit ftpClient: FTPClient): immutable.Seq[FtpFile] =
    ftpClient.listFiles().map { file =>
      FtpFile(file.getName)
    }.toVector
}
