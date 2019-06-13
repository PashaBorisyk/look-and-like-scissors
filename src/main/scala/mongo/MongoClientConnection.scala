package mongo

import org.mongodb.scala.bson.collection.immutable.Document
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.{MongoClient, MongoCollection}


object MongoClientConnection {

   private val mongoClient = MongoClient("mongodb://localhost:27017")
   private val productDatabase = mongoClient.getDatabase("look-and-like")
   private val collection: MongoCollection[Document] = productDatabase.getCollection("products")

   def getByID(id: Int): Unit = {
      collection
         .find(and(equal("_id", id), equal("croppedImageUrl", null)))
         .subscribe((document: Document) => {

         }, (error: Throwable) => {

         })
   }

   def findAll() = collection.find()

}
