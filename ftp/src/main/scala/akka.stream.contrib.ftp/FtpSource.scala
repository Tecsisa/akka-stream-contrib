/*
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com>
 */
package akka.stream.contrib.ftp

import java.net.InetAddress
import akka.stream.{ Attributes, Outlet, SourceShape }
import akka.stream.impl.Stages.DefaultAttributes.IODispatcher
import akka.stream.Attributes.name
import akka.stream.contrib.ftp.FtpConnectionSettings.{ BasicFtpConnectionSettings, DefaultFtpPort }
import akka.stream.contrib.ftp.FtpCredentials.{ AnonFtpCredentials, NonAnonFtpCredentials }
import akka.stream.scaladsl.Source
import akka.stream.stage.{ GraphStageLogic, GraphStageWithMaterializedValue, OutHandler }
import org.apache.commons.net.ftp.FTPClient
import scala.concurrent.{ Future, Promise }
import scala.util.control.NonFatal

object FtpSource {

  final val SourceName = "FtpSource"

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

  def apply(connectionSettings: FtpConnectionSettings): Source[FtpFile, Future[Long]] = {
    implicit val ftpClient: FTPClient = new FTPClient
    Source
      .fromGraph(new FtpSource[FTPClient](connectionSettings))
      .withAttributes(name(SourceName) and IODispatcher)
  }

}

final class FtpSource[FtpClient] private (
  connectionSettings: FtpConnectionSettings
)(implicit ftpClient: FtpClient, ftpLike: FtpLike[FtpClient])
  extends GraphStageWithMaterializedValue[SourceShape[FtpFile], Future[Long]] {
  import FtpSource._

  val shape = SourceShape(Outlet[FtpFile](s"$SourceName.out"))

  def createLogicAndMaterializedValue(inheritedAttributes: Attributes) = {

    val matValuePromise = Promise[Long]()

    val logic = new GraphStageLogic(shape) {
      import shape._

      private var handler: ftpLike.Handler = _
      private var buffer: Vector[FtpFile] = Vector.empty
      private var numFilesTotal: Long = 0L

      override def preStart(): Unit = {
        super.preStart()
        try {
          handler = ftpLike.connect(connectionSettings)
          fillBuffer()
        } catch {
          case NonFatal(t) =>
            disconnect()
            matFailure(t)
            failStage(t)
        }
      }

      override def postStop(): Unit = {
        disconnect()
        matSuccess()
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
            disconnect()
          } finally {
            complete(out)
          }
        } // end of onPull

        override def onDownstreamFinish(): Unit = try {
          disconnect()
        } finally {
          matSuccess()
          super.onDownstreamFinish()
        }
      }) // end of handler

      private[this] def fillBuffer(): Unit =
        if (buffer.isEmpty) {
          buffer ++= ftpLike.listFiles(handler)
        }

      private[this] def disconnect(): Unit =
        ftpLike.disconnect(handler)

      private[this] def matSuccess(): Boolean =
        matValuePromise.trySuccess(numFilesTotal)

      private[this] def matFailure(t: Throwable): Boolean =
        matValuePromise.tryFailure(t)

    } // end of stage logic

    (logic, matValuePromise.future)
  }
}
