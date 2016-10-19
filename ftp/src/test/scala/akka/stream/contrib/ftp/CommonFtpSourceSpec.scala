/*
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com>
 */
package akka.stream.contrib.ftp

import akka.stream.scaladsl.Keep
import akka.stream.testkit.scaladsl.TestSink

class FtpSourceSpec extends CommonFtpSourceSpec with BaseFtpSpec
class SFtpSourceSpec extends CommonFtpSourceSpec with BaseSFtpSpec

trait CommonFtpSourceSpec extends BaseSpec {

  "FtpSource" should {
    "materialize to the total number of files" in {
      val numFilesExpected = 30
      generateFiles(numFilesExpected)
      val (totalFiles, probe) =
        createSource()
          .toMat(TestSink.probe)(Keep.both)
          .run()
      probe
        .request(40) // more demand than existing files
        .expectNextN(numFilesExpected.toLong)

      totalFiles.futureValue shouldBe numFilesExpected
    }
  }

}
