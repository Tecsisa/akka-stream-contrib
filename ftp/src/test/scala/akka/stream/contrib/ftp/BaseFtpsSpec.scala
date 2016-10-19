/*
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com>
 */
package akka.stream.contrib.ftp

import org.apache.ftpserver.FtpServerFactory
import org.apache.ftpserver.listener.ListenerFactory
import org.apache.ftpserver.ssl.SslConfigurationFactory
import java.io.File

trait BaseFtpsSpec extends BaseFtpSpec {
  import akka.stream.scaladsl.Source

  import scala.concurrent.Future

  final val AuthValueSsl = "SSL"
  final val AuthValueTls = "TLS"

  final val FtpServerKeystore =
    new File(getClass.getClassLoader.getResource("server.jks").getFile)
  final val FtpServerKeyStorePassword = "password"

  override protected def createSource(): Source[FtpFile, Future[Long]] =
    FtpsSource("localhost", port.getOrElse(BasePort))

  override protected def createFtpServerFactory(port: Option[Int]): FtpServerFactory = {
    assert(FtpServerKeystore.exists)
    val serverFactory = super.createFtpServerFactory(port)
    val listenerFactory = new ListenerFactory(serverFactory.getListener(DefaultListener))
    listenerFactory.setImplicitSsl(useImplicit)
    listenerFactory.setSslConfiguration(sslConfiguration.createSslConfiguration())
    serverFactory.addListener(DefaultListener, listenerFactory.createListener())
    serverFactory
  }

  def clientAuth: String = "none"

  def authValue: String

  def useImplicit: Boolean

  private[this] val sslConfiguration = {
    val sslConfigFactory: SslConfigurationFactory = new SslConfigurationFactory
    sslConfigFactory.setSslProtocol(authValue)

    sslConfigFactory.setKeystoreFile(FtpServerKeystore)
    sslConfigFactory.setKeystoreType("JKS")
    sslConfigFactory.setKeystoreAlgorithm("SunX509")
    sslConfigFactory.setKeyPassword(FtpServerKeyStorePassword)
    sslConfigFactory.setKeystorePassword(FtpServerKeyStorePassword)

    sslConfigFactory.setClientAuthentication(clientAuth)

    sslConfigFactory.setTruststoreFile(FtpServerKeystore)
    sslConfigFactory.setTruststoreType("JKS")
    sslConfigFactory.setTruststoreAlgorithm("SunX509")
    sslConfigFactory.setTruststorePassword(FtpServerKeyStorePassword)

    sslConfigFactory
  }

}
