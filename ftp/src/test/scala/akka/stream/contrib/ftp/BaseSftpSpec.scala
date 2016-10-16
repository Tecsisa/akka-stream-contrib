/*
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com>
 */
package akka.stream.contrib.ftp

import akka.stream.scaladsl.Source
import org.apache.sshd.server.SshServer
import org.apache.sshd.server.auth.password.PasswordAuthenticator
import org.apache.sshd.server.auth.pubkey.PublickeyAuthenticator
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider
import org.apache.sshd.server.session.ServerSession
import org.apache.sshd.server.scp.ScpCommandFactory
import org.apache.sshd.server.subsystem.sftp.SftpSubsystemFactory
import scala.concurrent.Future
import java.security.PublicKey

trait BaseSftpSpec extends BaseSpec {

  protected var sshd: SshServer = null

  final def createSource(): Source[FtpFile, Future[Long]] =
    FtpSource("localhost", port.getOrElse(BasePort)) // TODO

  final def startServer(): Unit = {
    sshd = SshServer.setUpDefaultServer()
    sshd.setPort(port.getOrElse(BasePort))
    sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(KeyPairProviderFile))
    import scala.collection.JavaConversions.seqAsJavaList
    sshd.setSubsystemFactories(new SftpSubsystemFactory :: Nil)
    sshd.setCommandFactory(new ScpCommandFactory)
    val passwordAuthenticator = new PasswordAuthenticator {
      def authenticate(username: String, password: String, session: ServerSession): Boolean =
        username != null && username == password
    }
    sshd.setPasswordAuthenticator(passwordAuthenticator)
    val publickeyAuthenticator = new PublickeyAuthenticator {
      def authenticate(username: String, key: PublicKey, session: ServerSession): Boolean = true
    }
    sshd.setPublickeyAuthenticator(publickeyAuthenticator)
    sshd.start()
  }

  final def stopServer(): Unit = {
    if (sshd != null) {
      sshd.stop(true)
      sshd = null
    }
  }
}
