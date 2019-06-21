
import actors.queue.KafkaConsumer
import actors.web.ImageDownloader
import akka.actor.ActorSystem
import org.opencv.core._

object Main {

   def main(args: Array[String]): Unit = {
      loadOpenCV_Lib()
      println("Creating actor system")
      val actorSystem = ActorSystem("image-processing-system")
      println("Actor system created and started")

      val downloaderActor = actorSystem.actorOf(ImageDownloader.props(), "downloader-actor")
      val kafkaConsumer = KafkaConsumer(actorSystem)
      kafkaConsumer.startConsuming { doc =>
         downloaderActor ! ImageDownloader.DownloadImage(doc)
      }
   }


   @throws[Exception]
   def loadOpenCV_Lib() = { // get the model
      val model = System.getProperty("sun.arch.data.model")
      // the path the .dll lib location
      var libraryPath = "C:/Users/pavel.borissiouk/Downloads/opencv/build/java/x86"
      // check for if system is 64 or 32
      if (model == "64") libraryPath = "C:/Users/pavel.borissiouk/Downloads/opencv/build/java/x64"
      // set the path
      System.setProperty("java.library.path", libraryPath)
      val sysPath = classOf[ClassLoader].getDeclaredField("sys_paths")
      sysPath.setAccessible(true)
      sysPath.set(null, null)
      // load the lib
      System.loadLibrary(Core.NATIVE_LIBRARY_NAME)
   }
}
