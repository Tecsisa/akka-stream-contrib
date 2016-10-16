/*
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com>
 */
package akka.stream.contrib.ftp

import akka.stream.{ Attributes, SourceShape, Outlet }
import akka.stream.stage.{ GraphStageLogic, GraphStageWithMaterializedValue, OutHandler }
import scala.concurrent.{ Future, Promise }
import scala.util.control.NonFatal

/**
 * @author Juan José Vázquez Delgado
 * @tparam FtpClient
 */
trait FtpSourceGeneric[FtpClient]
  extends GraphStageWithMaterializedValue[SourceShape[FtpFile], Future[Long]] {

  def name: String

  def connectionSettings: FtpConnectionSettings

  implicit def ftpClient: FtpClient

  val ftpLike: FtpLike[FtpClient]

  val shape = SourceShape(Outlet[FtpFile](s"$name.out"))

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
