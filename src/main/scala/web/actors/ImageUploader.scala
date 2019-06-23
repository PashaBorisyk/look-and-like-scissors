package web.actors

import java.io.FileOutputStream

import akka.actor.{Actor, ActorLogging, Props}
import azure.BlobStorageHelper
import mongo.MongoClientConnection
import org.mongodb.scala.bson.collection.immutable.Document

object ImageUploader {

   def props() = Props(new ImageUploader)

   final case class UploadImage(documentWithSource: (Document, Array[Byte]))

}

class ImageUploader extends Actor with ActorLogging {

   override def receive = {
      case ImageUploader.UploadImage((document, source)) =>
         upload(document, source)
      case t => throw new RuntimeException(s"Unknown message type: $t")

   }

   var s = 1

   def upload(document: Document, source: Array[Byte]): Unit = {
      log.info("Starting image upload")
      s += 1
      val fileOutputStream = new FileOutputStream(s"./img/some$s.png")
      fileOutputStream.write(source)
      fileOutputStream.flush()
      fileOutputStream.close()
      log.info("Image upload finished")
      log.info("============================================================")
      BlobStorageHelper.upload(source).subscribe { result =>
         if (result.statusCode() == 201) {
            val id = document("_id").asObjectId()
            val imageURL = result.request().url().toExternalForm
            MongoClientConnection.update(id, imageURL).subscribe(
               (error: Throwable) => {
                  error.printStackTrace()
                  log.error(error, s"Document with id $id was not updated")
               }, () => log.info(s"Document with id $id updated")
            )
         } else {
            log.info(s"No document $document will not be updated. Azure response status : ${result.statusCode()}")
         }
      }
   }

}
