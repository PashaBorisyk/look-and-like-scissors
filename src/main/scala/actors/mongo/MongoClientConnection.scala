package actors.mongo

import org.mongodb.scala.bson.collection.immutable.Document
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.{MongoClient, MongoCollection}

import scala.concurrent.{Future, Promise}

object MongoClientConnection {

   private val mongoClient = MongoClient("mongodb://localhost:27017")
   private val productDatabase = mongoClient.getDatabase("look-and-like")
   private val collection: MongoCollection[Document] = productDatabase.getCollection("products")

   def getByID(id: String, promise: Promise[Document]): Future[Document] = {
      collection
         .find(and(equal("_id", id), equal("croppedImageUrl", null)))
         .subscribe((document: Document) => {
            promise.success(document)
         }, (error: Throwable) => {
            println(s"Error getting document with id $id form database")
            promise.failure(error)
         })

      promise.future
   }

   def findAll() = collection.find()

}
