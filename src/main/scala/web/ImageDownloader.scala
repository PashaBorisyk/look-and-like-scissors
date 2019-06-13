package web

import java.net.URL
import java.util.concurrent.Executors

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import image.ImageCoder
import org.mongodb.scala.bson.collection.immutable.Document
import sun.misc.IOUtils
import web.ImageDownloader.DownloadImage

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object ImageDownloader {

   def props(imageCoderActor: ActorRef) = Props(new ImageDownloader(imageCoderActor))

   final case class DownloadImage(document: Document)

}

class ImageDownloader(imageCoderActor: ActorRef) extends Actor with ActorLogging {

   implicit val ec: ExecutionContext = ExecutionContext.fromExecutor(Executors.newCachedThreadPool())

   override def receive = {
      case DownloadImage(document) =>
         getProductImageBytes(document).onComplete {
            case Failure(exception) => log.error(exception, "Error downloading image")
            case Success(bytes) => imageCoderActor ! ImageCoder.DecodeImage(document -> bytes)
         }
      case _ => throw new RuntimeException("Unknown type of operation")
   }

   def getProductImageBytes(document: Document): Future[Array[Byte]] = {
      val url = getUrl(document)
      download(url)
   }

   private def getUrl(document: Document) = {
      val shopName = if (document("shopName").isString)
         document("shopName").asString().getValue
      else throw new RuntimeException("Product must contain shopName field as String type")

      val images = getImagesArray(document)

      shopName.toLowerCase match {
         case "zara" => "http:"+images.last
         case "h&m" => "http:"+images(images.length-2)
      }
   }

   private def getImagesArray(document: Document): Array[String] = {
      if (document("images").isArray) {
         val imagesBsonArray = document("images").asArray()
         if (imagesBsonArray.isEmpty)
            throw new RuntimeException("Image array must not be empty")

         val imagesStringArray = new Array[String](imagesBsonArray.size())

         for (i <- 0 until imagesBsonArray.size()) {
            imagesStringArray(i) = imagesBsonArray.get(i).asString().getValue
         }
         if (imagesStringArray.isEmpty)
            throw new RuntimeException("imagesStringArray must not be empty")
         imagesStringArray
      } else {
         throw new RuntimeException("Product must contain images")
      }
   }

   private def download(url: String) = Future {
      if (url.isEmpty)
         Array.emptyByteArray
      else {
         log.info(s"Starting image download. Url: $url")
         val urlObj = new URL(url)
         val imageInBytes = IOUtils.readFully(urlObj.openStream(), Int.MaxValue, false)
         log.info(s"Image successfully downloaded. Image size in bytes: ${imageInBytes.size}")
         imageInBytes
      }
   }

}
