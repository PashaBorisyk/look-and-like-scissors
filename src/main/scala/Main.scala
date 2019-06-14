
import akka.actor.ActorSystem
import image.{ImageCoder, ImageProcessor}
import mongo.MongoClientConnection
import org.mongodb.scala.bson.collection.immutable.Document
import org.opencv.core._
import org.opencv.imgcodecs.Imgcodecs
import web.{ImageDownloader, ImageUploader}

object Main {

   def main(args: Array[String]): Unit = {
      println("Creating actor system")
      val actorSystem = ActorSystem("iot-system")
      loadOpenCV_Lib()

      val imageProcessorActor = actorSystem.actorOf(ImageProcessor.props(),"image-processor-actor")
      val uploaderActor = actorSystem.actorOf(ImageUploader.props(),"uploader-actor")
      val imageCoderActor = actorSystem.actorOf(
         ImageCoder.props(imageProcessorActor, uploaderActor),
         "image-coder-actor"
      )
      val downloaderActor = actorSystem.actorOf(ImageDownloader.props(imageCoderActor),"downloader-actor")

      val src = Imgcodecs.imread("C:\\pashaborisyk\\projects\\Scala\\look-and-like-scissors\\7647851420_6_1_1.jpg")

      MongoClientConnection.findAll().subscribe((document: Document) => {
         downloaderActor ! ImageDownloader.DownloadImage(document)
      }, (error: Throwable) => {

      })

      //
      //

      //      val result = getGradient2(src)
      //      Imgcodecs.imwrite("./save.png", result)
      println("Actor system created and started")
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
