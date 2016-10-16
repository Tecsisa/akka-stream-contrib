/*
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com>
 */
package akka.stream.contrib.ftp

import com.jcraft.jsch.JSch
import org.apache.commons.net.ftp.FTPClient
import scala.collection.immutable

/**
 * @author Juan José Vázquez Delgado
 */
trait FtpLike[FtpClient] {

  type Handler

  def connect(connectionSettings: FtpConnectionSettings)(implicit ftpClient: FtpClient): Handler

  def disconnect(handler: Handler)(implicit ftpClient: FtpClient): Unit

  def listFiles(handler: Handler): immutable.Seq[FtpFile]
}

object FtpLike {
  // type class instances
  implicit val ftpLikeInstance = new FtpLike[FTPClient] with Ftp
  implicit val sFtpLikeInstance = new FtpLike[JSch] with sFtp
}