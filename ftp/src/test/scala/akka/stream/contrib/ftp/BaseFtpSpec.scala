/*
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com>
 */
package akka.stream.contrib.ftp

import akka.stream.scaladsl.Source
import org.apache.ftpserver.FtpServer
import org.apache.ftpserver.{ ConnectionConfigFactory, FtpServerFactory }
import org.apache.ftpserver.filesystem.nativefs.NativeFileSystemFactory
import org.apache.ftpserver.listener.ListenerFactory
import org.apache.ftpserver.usermanager.{ ClearTextPasswordEncryptor, PropertiesUserManagerFactory }
import scala.concurrent.Future
import scala.util.control.NonFatal

trait BaseFtpSpec extends BaseSpec {

  final val DefaultListener = "default"

  protected var ftpServer: FtpServer = null

  final def createSource(): Source[FtpFile, Future[Long]] =
    FtpSource("localhost", port.getOrElse(BasePort))

  final def startServer(): Unit = {
    val factory = createFtpServerFactory(port)
    if (factory != null) {
      ftpServer = factory.createServer()
      if (ftpServer != null) {
        ftpServer.start()
      }
    }
  }

  final def stopServer(): Unit = {
    if (ftpServer != null) {
      try {
        ftpServer.stop()
        ftpServer = null
      } catch {
        case NonFatal(t) => // ignore
      }
    }
  }

  private[this] def createFtpServerFactory(port: Option[Int]) = {

    assert(UsersFile.exists())

    val fsf = new NativeFileSystemFactory
    fsf.setCreateHome(true)

    val pumf = new PropertiesUserManagerFactory
    pumf.setAdminName("admin")
    pumf.setPasswordEncryptor(new ClearTextPasswordEncryptor)
    pumf.setFile(UsersFile)
    val userMgr = pumf.createUserManager()

    val factory = new ListenerFactory
    factory.setPort(port.getOrElse(BasePort))

    val serverFactory = new FtpServerFactory
    serverFactory.setUserManager(userMgr)
    serverFactory.setFileSystem(fsf)
    serverFactory.setConnectionConfig(new ConnectionConfigFactory().createConnectionConfig())
    serverFactory.addListener(DefaultListener, factory.createListener())

    serverFactory
  }

}
