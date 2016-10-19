/*
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com>
 */
package akka.stream.contrib.ftp

import akka.stream.contrib.ftp.FtpConnectionSettings.SshBasicFtpConnectionSettings
import com.jcraft.jsch.{ ChannelSftp, JSch }
import scala.collection.immutable

/**
 * @author Juan José Vázquez Delgado
 */
trait SFtp {
  _: FtpLike[JSch] =>

  type Handler = ChannelSftp

  def connect(connectionSettings: FtpConnectionSettings)(implicit ftpClient: JSch): Handler = {
    val session = ftpClient.getSession(
      connectionSettings.credentials.username,
      connectionSettings.host.getHostAddress,
      connectionSettings.port
    )
    session.setPassword(connectionSettings.credentials.password) // TODO
    connectionSettings match {
      case SshBasicFtpConnectionSettings(_, _, _, strictHostKeyChecking) =>
        val config = new java.util.Properties
        config.setProperty("StrictHostKeyChecking", if (strictHostKeyChecking) "yes" else "no")
        session.setConfig(config)
      case _ => // ignore
    }
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
    val entries =
      handler.ls(".").toSeq.filter { case entry: Handler#LsEntry => !entry.getAttrs.isDir } // TODO
    entries.map {
      case entry: Handler#LsEntry => FtpFile(entry.getFilename)
      case _                      => FtpFile("") // TODO
    }.toVector
  }
}
