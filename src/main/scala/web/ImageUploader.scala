package web

import java.io.FileOutputStream

import akka.actor.{Actor, ActorLogging, Props}
import org.mongodb.scala.bson.collection.immutable.Document

object ImageUploader {

   def props() = Props(new ImageUploader)

   final case class UploadImage(documentWithSource: (Document,Array[Byte]))
}

class ImageUploader extends Actor with ActorLogging {

   override def receive = {
      case ImageUploader.UploadImage((document,source)) =>
         upload(source)
      case _ => throw new RuntimeException("Unknown type of operation")

   }

   var i = 0

   def upload(source: Array[Byte]) : String = {
      log.info("Starting image upload")
      i+=i
      if(i < 100) {
         val fileOutputStream = new FileOutputStream(s"./img/some$i.png")
         fileOutputStream.write(source)
         fileOutputStream.flush()
         fileOutputStream.close()
         log.info("Image upload finished")
      }
      "./some.png"
   }

}
