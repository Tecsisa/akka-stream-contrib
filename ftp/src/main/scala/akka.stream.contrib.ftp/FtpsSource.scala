/*
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com>
 */
package akka.stream.contrib.ftp

import akka.stream.contrib.ftp.FtpConnectionSettings.{ BasicFtpConnectionSettings, DefaultFtpPort }
import akka.stream.contrib.ftp.FtpCredentials.{ AnonFtpCredentials, NonAnonFtpCredentials }
import akka.stream.scaladsl.Source
import org.apache.commons.net.ftp.FTPClient
import scala.concurrent.Future
import java.net.InetAddress

/**
 * @author Juan José Vázquez Delgado
 */
object FtpsSource extends FtpSourceFactory[FtpsSource] {
  final val SourceName = "FtpsSource"

  val sourceName = SourceName

  def createSource(connectionSettings: FtpConnectionSettings): FtpsSource =
    FtpsSource(sourceName, connectionSettings)

  def apply(hostname: String): Source[FtpFile, Future[Long]] = apply(hostname, DefaultFtpPort)

  def apply(hostname: String, port: Int): Source[FtpFile, Future[Long]] =
    apply(BasicFtpConnectionSettings(InetAddress.getByName(hostname), port, AnonFtpCredentials))

  def apply(hostname: String, username: String, password: String): Source[FtpFile, Future[Long]] =
    apply(hostname, DefaultFtpPort, username, password)

  def apply(hostname: String, port: Int, username: String, password: String): Source[FtpFile, Future[Long]] =
    apply(
      BasicFtpConnectionSettings(
        InetAddress.getByName(hostname),
        port,
        NonAnonFtpCredentials(username, password)
      )
    )
}

final case class FtpsSource(
  name:               String,
  connectionSettings: FtpConnectionSettings
)(implicit val ftpLike: FtpLike[FTPClient]) extends FtpSourceGeneric[FTPClient] {
  val ftpClient: FTPClient = new FTPClient
}
