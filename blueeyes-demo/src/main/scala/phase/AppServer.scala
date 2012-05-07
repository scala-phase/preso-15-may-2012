package phase

import blueeyes.core.data._
import blueeyes.core.service.engines.NettyEngine
import blueeyes.core.service.{HttpServer, HttpReflectiveServiceList}
import net.lag.configgy.Configgy
import java.util.concurrent.CountDownLatch

object AppServer extends HttpServer with HttpReflectiveServiceList[ByteChunk] with NettyEngine with PostService { self =>

  override def main(args: Array[String]) {

    val configString = """
      server.port = 8080
      server.sslEnable = false
    """

    Configgy.configureFromString(configString)

    start.deliverTo { _ =>
      Runtime.getRuntime.addShutdownHook { new Thread {
        override def start() {
          val doneSignal = new CountDownLatch(1)

          self.stop.deliverTo { _ =>
            doneSignal.countDown()
          }.ifCanceled { e =>
            doneSignal.countDown()
          }

          doneSignal.await()
        }
      }}
    }
  }
}
