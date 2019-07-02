
import akka.actor.ActorSystem
import mongo.MongoClientConnection
import org.mongodb.scala.bson.collection.immutable.Document
import org.opencv.core._
import queue.KafkaClient
import web.actors.ImageDownloader

object Main {

   implicit val actorSystem: ActorSystem = ActorSystem("image-processing-system")
   val imageDownloader = actorSystem.actorOf(ImageDownloader.props(), "image-downloader-actor")

   def main(args: Array[String]): Unit = {

      loadOpenCV_Lib()
      println("Creating actor system")
      println("Actor system created and started")

      val kafkaClientActor = actorSystem.actorOf(KafkaClient.props({ doc =>
         imageDownloader ! ImageDownloader.DownloadImage(doc)
      }), "kafka-client")


//      MongoClientConnection.documentsCount.subscribe(
//         (count: Long) =>
//            attachBGImages(20, count, 0), (error: Throwable) => error.printStackTrace()
//      )

   }

   def attachBGImages(limit: Long, total: Long, done: Int): Unit = {

      println(s"New iteration started: limit: $limit; total: $total; done: $done")

      if (total <= done)
         return

      val numberOfDocuments = if (total - done > limit) limit else total - done

      var doneInThisSession = 0

      MongoClientConnection.findWithoutBGImage(limit).subscribe((doc: Document) => {
         imageDownloader ! ImageDownloader.DownloadImage(doc)
         doneInThisSession += 1
      }, (error: Throwable) => {
         error.printStackTrace()
      }, () => attachBGImages(numberOfDocuments, total, done + doneInThisSession))
   }

   def testFunc(actorSystem: ActorSystem): Unit = {


      MongoClientConnection.findByImageURL("https://lookandlikeimages.blob.core.windows.net/images/Dsy0vcINTIV1evKPs2F1AtF7W5aUR6QsbUwnfX8qdGTqSkTaoIt1vxNwbTSw.png").subscribe((doc: Document) => {
         println(doc)
      }, (error: Throwable) => {
         error.printStackTrace()
      })
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
