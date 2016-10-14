/*
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com>
 */
package akka.stream.contrib.ftp

import com.jcraft.jsch.JSch

trait sFtp {
  _: FtpLike[JSch] =>

}
