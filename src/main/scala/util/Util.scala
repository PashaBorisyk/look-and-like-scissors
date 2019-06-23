package util

import java.util.Random

import org.mongodb.scala.bson.BsonArray
import org.mongodb.scala.bson.collection.immutable.Document

object Util {

   private final val NAME_LENGTH = 60

   private final val random = new Random
   private final val charsUTF8 = Array(
      '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e',
      'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't',
      'u', 'v', 'w', 'x', 'y', 'z', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I',
      'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X',
      'Y', 'Z'
   )

   def getShopName(document: Document) = {
      document("metaInformation").asDocument().get("shopName").asString().getValue.toLowerCase
   }

   def convertToStringArray(bsonArray: BsonArray) = {
      val stringArray = new Array[String](bsonArray.size())
      for (i <- 0 until bsonArray.size()){
         stringArray(i) = bsonArray.get(i).asString().getValue
      }
      stringArray
   }

   def generateFileName(postfix: String): String = {

      val name = (0 until NAME_LENGTH).map { _ =>
         charsUTF8(random.nextInt(charsUTF8.length - 1))
      }
      new String(name.toArray) + "." + postfix
   }

}
