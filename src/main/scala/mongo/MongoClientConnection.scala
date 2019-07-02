package mongo

import java.text.SimpleDateFormat
import java.util.Date

import org.mongodb.scala.bson.BsonObjectId
import org.mongodb.scala.bson.collection.immutable.Document
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.Updates
import org.mongodb.scala.{MongoClient, MongoCollection}

import scala.concurrent.{Future, Promise}

object MongoClientConnection {

   private final val mongoClient = MongoClient("mongodb://look-and-like-test:unE8DZr3T7yA6SLDPjknaT8Bj0MzLD4O4604EDq0OE44Lv9BxAslwWXTqLvJFzvqLCBoCDshGgUUJuKoahpT6w==@look-and-like-test.documents.azure.com:10255/?ssl=true&replicaSet=globaldb")
   private final val productDatabase = mongoClient.getDatabase("look-and-like-test")
   private final val collection: MongoCollection[Document] = productDatabase.getCollection("products")

   private final val updateDateFromat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX")


   def getByIDString(id: String): Future[Document] = {
      val promise = Promise[Document]
      collection
         .find(
            and(
               equal("_id", BsonObjectId(id)),
               or(
                  equal("data.images.noBackgroundImageUrl", null),
                  equal("data.images.noBackgroundImageUrl", "")
               )
            )
         )
         .subscribe((document: Document) => {
            promise.success(document)
         }, (error: Throwable) => {
            println(s"Error getting document with id $id form database")
            promise.failure(error)
         })

      promise.future
   }

   def update(id: BsonObjectId, imageWithoutBgURL: String) =
      collection.updateOne(equal("_id", id), Updates.combine(
         Updates.set("data.images.noBackgroundImageUrl", imageWithoutBgURL),
         Updates.set("metaInformation.updatedAt", updateDateFromat.format(new Date()))
      ))


   def findWithoutBGImage(limit: Long) = collection.find(or(
      equal("data.images.noBackgroundImageUrl", null),
      equal("data.images.noBackgroundImageUrl", "")
   )).limit(limit.toInt)

   def findByImageURL(url: String) =
      collection.find(equal("data.images.noBackgroundImageUrl", url))

   def documentsCount = collection.countDocuments()

}
