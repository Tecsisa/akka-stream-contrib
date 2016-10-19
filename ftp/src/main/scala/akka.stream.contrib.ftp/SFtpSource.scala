/*
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com>
 */
package akka.stream.contrib.ftp

import akka.stream.impl.Stages.DefaultAttributes.IODispatcher
import akka.stream.Attributes.name
import akka.stream.contrib.ftp.FtpConnectionSettings.{ SshBasicFtpConnectionSettings, DefaultFtpPort }
import akka.stream.contrib.ftp.FtpCredentials.{ AnonFtpCredentials, NonAnonFtpCredentials }
import akka.stream.scaladsl.Source
import com.jcraft.jsch.JSch
import scala.concurrent.Future
import java.net.InetAddress

/**
 * @author Juan José Vázquez Delgado
 */
object SFtpSource {

  final val SourceName = "SFtpSource"

  def apply(hostname: String): Source[FtpFile, Future[Long]] = apply(hostname, DefaultFtpPort)

  def apply(hostname: String, port: Int): Source[FtpFile, Future[Long]] =
    apply(SshBasicFtpConnectionSettings(
      InetAddress.getByName(hostname), port, AnonFtpCredentials, strictHostKeyChecking = false // TODO
    ))

  def apply(hostname: String, username: String, password: String): Source[FtpFile, Future[Long]] =
    apply(hostname, DefaultFtpPort, username, password)

  def apply(hostname: String, port: Int, username: String, password: String): Source[FtpFile, Future[Long]] =
    apply(
      SshBasicFtpConnectionSettings(
        InetAddress.getByName(hostname),
        port,
        NonAnonFtpCredentials(username, password),
        strictHostKeyChecking = false // TODO
      )
    )

  def apply(connectionSettings: FtpConnectionSettings): Source[FtpFile, Future[Long]] =
    Source
      .fromGraph(SFtpSource(SourceName, connectionSettings))
      .withAttributes(name(SourceName) and IODispatcher)

}

final case class SFtpSource(
  name:               String,
  connectionSettings: FtpConnectionSettings
)(implicit val ftpLike: FtpLike[JSch]) extends FtpSourceGeneric[JSch] {
  val ftpClient: JSch = new JSch
}
