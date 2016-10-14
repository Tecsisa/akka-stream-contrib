/*
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com>
 */
package akka.stream.contrib.ftp

import org.apache.commons.net.ftp.{ FTPClient, FTPListParseEngine }
import scala.collection.immutable

trait Ftp {

  type FtpIterator = FTPListParseEngine

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

  def hasNext(iterator: FtpIterator): Boolean = iterator.hasNext

  def initIterator(implicit ftpClient: FTPClient): FtpIterator =
    ftpClient.initiateListParsing()

  def next(iterator: FtpIterator, size: Int): immutable.Seq[FtpFile] =
    iterator.getNext(size).map { file =>
      FtpFile(file.getName)
    }.toList
}
