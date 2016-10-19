/*
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com>
 */
package akka.stream.contrib.ftp

import java.io.{ File, FileOutputStream }
import java.nio.file.{ Files, Paths }
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import org.apache.mina.util.AvailablePortFinder
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{ BeforeAndAfterAll, Matchers, WordSpec }
import scala.concurrent.{ Await, Future }
import scala.concurrent.duration.DurationInt
import scala.util.Try

trait BaseSpec extends WordSpec with Matchers with BeforeAndAfterAll with ScalaFutures {

  final val BasePort = 21000
  final val FtpRootDir = "target/res/home"
  final val UsersFile = new File(getClass.getClassLoader.getResource("users.properties").getFile)
  final val KeyPairProviderFile =
    new File(getClass.getClassLoader.getResource("hostkey.pem").getFile)

  protected implicit val system = ActorSystem("default")
  protected implicit val mat = ActorMaterializer()

  protected val port: Option[Int] = Try(AvailablePortFinder.getNextAvailable(BasePort)).toOption

  protected def startServer(): Unit

  protected def stopServer(): Unit

  protected def createSource(): Source[FtpFile, Future[Long]]

  override protected def beforeAll() = {
    super.beforeAll()
    deleteDirectory(FtpRootDir)
    Files.createDirectories(Paths.get(FtpRootDir))
    startServer()
  }

  override protected def afterAll() = {
    stopServer()
    Await.ready(system.terminate(), 42.seconds)
    deleteDirectory(FtpRootDir)
    super.afterAll()
  }

  protected[ftp] def generateFiles(numFiles: Int = 30) =
    (1 to numFiles).foreach { i =>
      putFileOnFtp(s"$FtpRootDir/sample_$i")
    }

  private[this] def deleteDirectory(file: String): Unit =
    deleteDirectory(new File(file))

  private[this] def deleteDirectory(file: File): Unit = {
    if (file.isDirectory)
      file.listFiles.foreach(deleteDirectory)
    file.delete()
  }

  private[this] def putFileOnFtp(path: String) = {
    val fos = new FileOutputStream(path)
    fos.write(loremIpsum)
    fos.close()
  }

  private[this] def loremIpsum =
    """|Lorem ipsum dolor sit amet, consectetur adipiscing elit. Praesent auctor imperdiet
       |velit, eu dapibus nisl dapibus vitae. Sed quam lacus, fringilla posuere ligula at,
       |aliquet laoreet nulla. Aliquam id fermentum justo. Aliquam et massa consequat,
       |pellentesque dolor nec, gravida libero. Phasellus elit eros, finibus eget
       |sollicitudin ac, consectetur sed ante. Etiam ornare lacus blandit nisi gravida
       |accumsan. Sed in lorem arcu. Vivamus et eleifend ligula. Maecenas ut commodo ante.
       |Suspendisse sit amet placerat arcu, porttitor sagittis velit. Quisque gravida mi a
       |porttitor ornare. Cras lorem nisl, sollicitudin vitae odio at, vehicula maximus
       |mauris. Sed ac purus ac turpis pellentesque cursus ac eget est. Pellentesque
       |habitant morbi tristique senectus et netus et malesuada fames ac turpis egestas.
       |""".stripMargin.toCharArray.map(_.toByte)
}
