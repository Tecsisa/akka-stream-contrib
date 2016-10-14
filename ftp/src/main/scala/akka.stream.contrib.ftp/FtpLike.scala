/*
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com>
 */
package akka.stream.contrib.ftp

import org.apache.commons.net.ftp.FTPClient
import scala.collection.immutable

trait FtpLike[FtpClient] {

  type FtpIterator

  def connect(connectionSettings: FtpConnectionSettings)(implicit ftpClient: FtpClient): Unit

  def disconnect()(implicit ftpClient: FtpClient): Unit

  def hasNext(iterator: FtpIterator): Boolean

  def initIterator(implicit ftpClient: FtpClient): FtpIterator

  def next(iterator: FtpIterator, size: Int): immutable.Seq[FtpFile]
}

object FtpLike {
  // type class instances
  implicit val ftpLikeInstance = new FtpLike[FTPClient] with Ftp

}