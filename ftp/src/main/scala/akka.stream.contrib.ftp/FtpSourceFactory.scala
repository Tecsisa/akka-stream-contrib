/*
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com>
 */
package akka.stream.contrib.ftp

import akka.stream.Attributes.name
import akka.stream.impl.Stages.DefaultAttributes.IODispatcher
import akka.stream.scaladsl.Source

import scala.concurrent.Future

/**
 * @author Juan José Vázquez Delgado
 */
trait FtpSourceFactory[A <: FtpSourceGeneric[_]] {

  def sourceName: String

  def createSource(connectionSettings: FtpConnectionSettings): A

  def apply(connectionSettings: FtpConnectionSettings): Source[FtpFile, Future[Long]] =
    Source
      .fromGraph(createSource(connectionSettings))
      .withAttributes(name(sourceName) and IODispatcher)
}
