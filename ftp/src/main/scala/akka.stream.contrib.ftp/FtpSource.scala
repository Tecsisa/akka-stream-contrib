/*
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com>
 */
package akka.stream.contrib.ftp

import java.net.InetAddress
import akka.stream.{ Attributes, Outlet, SourceShape }
import akka.stream.Attributes.name
import akka.stream.contrib.ftp.FtpConnectionSettings.{ BasicFtpConnectionSettings, DefaultFtpConnectionSettings, DefaultFtpPort }
import akka.stream.contrib.ftp.FtpCredentials.{ AnonFtpCredentials, NonAnonFtpCredentials }
import akka.stream.scaladsl.Source
import akka.stream.stage.{ GraphStageLogic, GraphStageWithMaterializedValue, OutHandler }
import org.apache.commons.net.ftp.FTPClient
import scala.concurrent.{ Future, Promise }
import scala.util.control.NonFatal

object FtpSource {

  final val SourceName = "FtpSource"

  def apply(): Source[FtpFile, Future[Long]] = apply(DefaultFtpConnectionSettings)

  def apply(hostname: String): Source[FtpFile, Future[Long]] = apply(hostname, DefaultFtpPort)

  def apply(hostname: String, port: Int): Source[FtpFile, Future[Long]] =
    apply(BasicFtpConnectionSettings(InetAddress.getByName(hostname), port, AnonFtpCredentials))

  def apply(hostname: String, username: String, password: String): Source[FtpFile, Future[Long]] =
    apply(hostname, DefaultFtpPort, username, password)

  def apply(hostname: String, port: Int, username: String, password: String): Source[FtpFile, Future[Long]] =
    apply(BasicFtpConnectionSettings(InetAddress.getByName(hostname), port, NonAnonFtpCredentials(username, password)))

  def apply(connectionSettings: FtpConnectionSettings): Source[FtpFile, Future[Long]] = {
    implicit val ftpClient: FTPClient = new FTPClient
    Source.fromGraph(new FtpSource[FTPClient](connectionSettings)).withAttributes(name(SourceName))
  }

}

final class FtpSource[FtpClient] private (
  connectionSettings: FtpConnectionSettings
)(implicit ftpClient: FtpClient, ftpLike: FtpLike[FtpClient])
  extends GraphStageWithMaterializedValue[SourceShape[FtpFile], Future[Long]] {
  import FtpSource._

  val matValue = Promise[Long]()

  val shape = SourceShape(Outlet[FtpFile](s"$SourceName.out"))

  def createLogicAndMaterializedValue(inheritedAttributes: Attributes) = {

    val logic = new GraphStageLogic(shape) {
      import shape._

      private var buffer: Vector[FtpFile] = Vector.empty
      private var numFilesTotal: Long = 0L

      override def preStart(): Unit = {
        super.preStart()
        try {
          ftpLike.connect(connectionSettings)
          fillBuffer()
        } catch {
          case NonFatal(t) =>
            ftpLike.disconnect()
            matFailure(t)
            failStage(t)
        }
      }

      override def postStop(): Unit = {
        ftpLike.disconnect()
        super.postStop()
      }

      setHandler(out, new OutHandler {
        def onPull(): Unit = {
          fillBuffer()
          buffer match {
            case Seq() =>
              finalize()
            case head +: Seq() =>
              push(out, head)
              numFilesTotal += 1
              finalize()
            case head +: tail =>
              push(out, head)
              numFilesTotal += 1
              buffer = tail
          }
          def finalize() = try {
            ftpLike.disconnect()
          } finally {
            matSuccess()
            complete(out)
          }
        } // end of onPull

        override def onDownstreamFinish(): Unit = try {
          ftpLike.disconnect()
        } finally {
          matSuccess()
          super.onDownstreamFinish()
        }
      }) // end of handler

      //      private[this] def eof: Boolean = !ftpIterator.exists(ftpLike.hasNext)

      private[this] def fillBuffer() =
        if (buffer.isEmpty) {
          buffer ++= ftpLike.listFiles()
        }

      private[this] def matSuccess() = matValue.success(numFilesTotal)

      private[this] def matFailure(t: Throwable) = matValue.failure(t)

    } // end of stage logic

    (logic, matValue.future)
  }
}
