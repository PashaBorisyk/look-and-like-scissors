package actors.web

import java.net.URL

import actors.image.ImageCoder
import actors.web.ImageDownloader.DownloadImage
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import org.mongodb.scala.bson.collection.immutable.Document
import sun.misc.IOUtils

object ImageDownloader {

   def props() = Props(new ImageDownloader())

   final case class DownloadImage(document: Document)

}

class ImageDownloader() extends Actor with ActorLogging {

   var imageCoderActor: ActorRef = Actor.noSender

   override def preStart() = {
      imageCoderActor = context.actorOf(ImageCoder.props(), "image-coder-actor")
   }

   override def receive = {
      case DownloadImage(document) =>
         val bytes = getProductImageBytes(document)
         if (bytes.nonEmpty)
            imageCoderActor ! ImageCoder.DecodeImage(document -> bytes)
         else
            log.info(s"No actors.image downloaded for product $document")

      case _ => throw new RuntimeException("Unknown type of operation")
   }

   def getProductImageBytes(document: Document): Array[Byte] = {
      val url = getUrl(document)
      if (url.nonEmpty)
         download(url)
      else
         Array.emptyByteArray
   }

   private def getUrl(document: Document) = {
      val data = document("data").asDocument
      val shopName = if (data.get("shopName").isString)
         data.get("shopName").asString().getValue
      else throw new RuntimeException("Product must contain shopName field as String type")

      val images = getImagesArray(document)

      shopName.toLowerCase match {
         case "zara" => "http:" + images.last
         case "h&m" if images.length > 2 => "http:" + images(images.length - 2)
         case _ => ""
      }
   }

   private def getImagesArray(document: Document): Array[String] = {
      if (document.containsKey("images") && document("images").isArray) {
         val imagesBsonArray = document("images").asArray()
         if (imagesBsonArray.isEmpty)
            throw new RuntimeException("Image array must not be empty")

         val imagesStringArray = new Array[String](imagesBsonArray.size())

         for (i <- 0 until imagesBsonArray.size()) {
            imagesStringArray(i) = imagesBsonArray.get(i).asString().getValue
         }
         if (imagesStringArray.isEmpty)
            throw new RuntimeException("ImagesStringArray must not be empty")
         imagesStringArray
      } else {
         throw new RuntimeException(s"Product must contain images : $document")
      }
   }

   private def download(url: String) = {
      if (url.isEmpty)
         Array.emptyByteArray
      else {
         log.info(s"Starting actors.image download. Url: $url")
         val urlObj = new URL(url)
         val imageInBytes = IOUtils.readFully(urlObj.openStream(), Int.MaxValue, false)
         log.info(s"Image successfully downloaded. Image size in bytes: ${imageInBytes.size}")
         imageInBytes
      }
   }

}
