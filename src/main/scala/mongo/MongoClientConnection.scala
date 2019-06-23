package mongo

import org.mongodb.scala.bson.BsonObjectId
import org.mongodb.scala.bson.collection.immutable.Document
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.Updates
import org.mongodb.scala.{MongoClient, MongoCollection}

import scala.concurrent.{Future, Promise}

object MongoClientConnection {

   private val mongoClient = MongoClient("")
   private val productDatabase = mongoClient.getDatabase("look-and-like-test")
   private val collection: MongoCollection[Document] = productDatabase.getCollection("products")

   def getByIDString(id: String, promise: Promise[Document]): Future[Document] = {
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
      collection.updateOne(equal("_id", id), Updates.set("data.images.noBackgroundImageUrl", imageWithoutBgURL))

   def findWithoutBGImage(limit:Long) = collection.find(or(
      equal("data.images.noBackgroundImageUrl", null),
      equal("data.images.noBackgroundImageUrl", "")
   )).limit(limit.toInt)

   def findByImageURL(url: String) =
      collection.find(equal("data.images.noBackgroundImageUrl", "https://lookandlikeimages.blob.core.windows.net/images/Dsy0vcINTIV1evKPs2F1AtF7W5aUR6QsbUwnfX8qdGTqSkTaoIt1vxNwbTSw.png"))

   def documentsCount = collection.countDocuments()

}
