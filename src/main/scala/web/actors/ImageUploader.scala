package web.actors

import java.io.FileOutputStream

import akka.actor.{Actor, ActorLogging, ActorSelection, Props}
import azure.BlobStorageHelper
import mongo.MongoClientConnection
import org.mongodb.scala.bson.collection.immutable.Document
import queue.KafkaClient

object ImageUploader {

   def props() = Props(new ImageUploader)

   final case class UploadImage(documentWithSource: (Document, Array[Byte]))

}

class ImageUploader extends Actor with ActorLogging {

   var kafkaClientActor:ActorSelection = _

   override def preStart() = {
      kafkaClientActor = context.system.actorSelection(context.system / "kafka-client")
   }

   override def receive = {
      case ImageUploader.UploadImage((document, source)) =>
         upload(document, source)
      case t => throw new RuntimeException(s"Unknown message type: $t")

   }

   def upload(document: Document, source: Array[Byte]): Unit = {
      log.info("Starting image upload")
      val result = BlobStorageHelper.upload(source).blockingGet()
      log.info("Image upload finished")
      log.info("============================================================")
      if (result.statusCode() == 201) {
         val id = document("_id").asObjectId()
         val imageURL = result.request().url().toExternalForm
         MongoClientConnection.setNoBGImageUrlForDocument(id, imageURL).subscribe(
            (error: Throwable) => {
               error.printStackTrace()
               log.error(error, s"Document with id $id was not updated")
            }, () => {
               log.info(s"Document with id $id updated")
               kafkaClientActor ! KafkaClient.Publish(id.getValue)
            }
         )
      } else {
         log.info(s"No document $document will not be updated. Azure response status : ${result.statusCode()}")
      }

   }

}
