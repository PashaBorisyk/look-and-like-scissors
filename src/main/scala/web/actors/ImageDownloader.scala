package web.actors

import java.net.URL

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import image.actors.ImageCoder
import org.mongodb.scala.bson.collection.immutable.Document
import sun.misc.IOUtils
import util.Util

object ImageDownloader {

   def props() = Props(new ImageDownloader())

   final case class DownloadImage(document: Document)

}

class ImageDownloader() extends Actor with ActorLogging {

   var imageCoderActor: ActorRef = Actor.noSender
   val downloadedURLs = collection.mutable.Set[String]()

   override def preStart() = {
      imageCoderActor = context.actorOf(ImageCoder.props(), "image-coder-actor")
   }

   override def receive = {
      case ImageDownloader.DownloadImage(document) =>
         val sources = getProductImageBytes(document)
         if (sources.nonEmpty)
            imageCoderActor ! ImageCoder.DecodeImages(document, sources)
         else
            log.info(s"No image downloaded for product $document")

      case t => throw new RuntimeException(s"Unknown message type : $t")
   }

   def getProductImageBytes(document: Document): Array[(String, Array[Byte])] = {
      val urls = getUrls(document)
      urls.map(download)
   }

   private def getUrls(document: Document) = {
      val data = document("data").asDocument
      val shopName = Util.getShopName(document)
      log.info(s"Getting image for document : $document")
      getUrlsFromDataDocument(data, shopName)
   }

   private def getUrlsFromDataDocument(data: Document, shopName: String): Array[String] = {
      if (containsImagesArray(data)) {

         val imagesBsonArray = data("images").asDocument().get("stockImageUrls").asArray()

         if (imagesBsonArray.isEmpty)
            throw new RuntimeException("Image array must not be empty")

         val urlsArray = Util.convertToStringArray(imagesBsonArray)

         val url = shopName match {
            case "h&m" => "http:" + urlsArray.find(_.contains("DESCRIPTIVESTILLLIFE")).getOrElse(
               throw new RuntimeException("h&m must contain at least one DESCRIPTIVESTILLLIFE image url"))
            case "zara" => "http:" + urlsArray.lastOption.getOrElse(
               throw new RuntimeException("Zara product should contain at least one image url"))
         }
         Array(url)
      } else {
         throw new RuntimeException(s"Product must contain images : $data")
      }
   }

   private def containsImagesArray(data: Document) = {
      data.containsKey("images") && data("images").asDocument().get("stockImageUrls").isArray
   }

   private def download(url: String) = {
      val source = if (url.isEmpty)
         Array.emptyByteArray
      else {
         log.info(s"Starting image download. Url: $url")
         val urlObj = new URL(url)
         val imageInBytes = IOUtils.readFully(urlObj.openStream(), Int.MaxValue, false)
         log.info(s"Image successfully downloaded. Image size in bytes: ${imageInBytes.size}")
         imageInBytes
      }
      url -> source
   }

}
