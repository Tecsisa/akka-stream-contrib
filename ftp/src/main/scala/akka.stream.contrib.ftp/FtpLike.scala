/*
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com>
 */
package akka.stream.contrib.ftp

import org.apache.commons.net.ftp.FTPClient
import scala.collection.immutable

trait FtpLike[FtpClient] {
  def connect(connectionSettings: FtpConnectionSettings)(implicit ftpClient: FtpClient): Unit

  def disconnect()(implicit ftpClient: FtpClient): Unit

  def listFiles()(implicit ftpClient: FtpClient): immutable.Seq[FtpFile]
}

object FtpLike {
  // type class instances
  implicit val ftpLikeInstance = new FtpLike[FTPClient] with Ftp

}