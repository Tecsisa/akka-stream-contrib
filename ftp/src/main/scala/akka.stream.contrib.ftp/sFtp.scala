/*
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com>
 */
package akka.stream.contrib.ftp

import com.jcraft.jsch.{ ChannelSftp, JSch }
import scala.collection.immutable

/**
 * @author Juan José Vázquez Delgado
 */
trait sFtp {
  _: FtpLike[JSch] =>

  type Handler = ChannelSftp

  def connect(connectionSettings: FtpConnectionSettings)(implicit ftpClient: JSch): Handler = {
    val session = ftpClient.getSession(
      connectionSettings.credentials.username,
      connectionSettings.host.getHostAddress,
      connectionSettings.port
    )
    session.connect()
    val channel = session.openChannel("sftp").asInstanceOf[ChannelSftp]
    channel.connect()
    channel
  }

  def disconnect(handler: Handler)(implicit ftpClient: JSch): Unit = {
    if (handler != null) {
      val session = handler.getSession
      if (session != null && session.isConnected) {
        session.disconnect()
      }
      if (handler.isConnected) {
        handler.disconnect()
      }
    }
  }

  def listFiles(handler: Handler): immutable.Seq[FtpFile] = {
    import scala.collection.JavaConversions.iterableAsScalaIterable
    handler.ls(".").map {
      case entry: handler.LsEntry => FtpFile(entry.getFilename)
      case _                      => sys.error("") // TODO
    }.toVector
  }
}
