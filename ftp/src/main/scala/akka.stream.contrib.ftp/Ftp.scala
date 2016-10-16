/*
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com>
 */
package akka.stream.contrib.ftp

import org.apache.commons.net.ftp.FTPClient
import scala.collection.immutable

/**
 * @author Juan José Vázquez Delgado
 */
trait Ftp {
  _: FtpLike[FTPClient] =>

  type Handler = FTPClient

  def connect(
    connectionSettings: FtpConnectionSettings
  )(implicit ftpClient: FTPClient): Handler = {
    if (ftpClient != null) {
      ftpClient.connect(connectionSettings.host, connectionSettings.port)
      ftpClient.login(
        connectionSettings.credentials.username,
        connectionSettings.credentials.password
      )
      ftpClient.enterLocalPassiveMode()
    }
    ftpClient
  }

  def disconnect(handler: Handler)(implicit ftpClient: FTPClient): Unit = {
    if (ftpClient != null) {
      if (ftpClient.isConnected) {
        ftpClient.logout()
        ftpClient.disconnect()
      }
    }
  }

  def listFiles(handler: Handler): immutable.Seq[FtpFile] =
    handler.listFiles().map { file =>
      FtpFile(file.getName)
    }.toVector
}
