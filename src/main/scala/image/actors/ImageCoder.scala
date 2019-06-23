package image.actors

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import org.mongodb.scala.bson.collection.immutable.Document
import org.opencv.core.{Mat, MatOfByte, Size}
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import util.Util
import web.actors.ImageUploader

object ImageCoder {

   def props(): Props = Props(new ImageCoder())

   final case class DecodeImages(document: Document, sources: Array[(String, Array[Byte])])

   final case class EncodeImage(document: Document, mat: Mat)

}

class ImageCoder extends Actor with ActorLogging {

   var uploaderActor: ActorRef = Actor.noSender
   var humanFilterActor: ActorRef = Actor.noSender
   var backgroundRemoverActor = Actor.noSender

   override def preStart() = {
      backgroundRemoverActor = context.actorOf(BackgroundRemover.props(), "background-remover-actor")
      humanFilterActor = context.actorOf(HumanFilter.props(backgroundRemoverActor), "human-filter-actor")
      uploaderActor = context.actorOf(ImageUploader.props(), "image-uploader-actor")
   }

   override def receive = {
      case ImageCoder.DecodeImages(document, sources) =>
         val images = decodeImages(sources)
         if (Util.getShopName(document) == "zara")
            humanFilterActor ! HumanFilter.DetectHumans(document, images)
         else {
            backgroundRemoverActor ! BackgroundRemover.ProcessFilter(document, images.lastOption.getOrElse(
               throw new RuntimeException("No image to process"))._2)
         }
      case ImageCoder.EncodeImage(document, image) =>
         uploaderActor ! ImageUploader.UploadImage(document -> encodeImage(image))

      case t => throw new RuntimeException(s"Unknown message type : $t")
   }

   private def decodeImages(imagesSources: Array[(String, Array[Byte])]): Array[(String, Mat)] = {
      imagesSources.map { case (url, source) =>
         val sourceImage = Imgcodecs.imdecode(new MatOfByte(source: _*), Imgcodecs.IMREAD_ANYCOLOR)
         val resizedImage = resizeImage(sourceImage)
         sourceImage.release()
         url -> resizedImage
      }
   }

   private def resizeImage(src: Mat): Mat = {
      val maxDim = Math.max(src.width(), src.height())
      if (maxDim > 1280) {

         val scale = if (src.width() >= src.height())
            1280 / src.width()
         else
            1280 / src.height()

         val dstImage = new Mat()
         Imgproc.resize(src, dstImage, new Size(src.width() * scale, src.height() * scale))
         dstImage
      } else {
         src
      }
   }


   private def encodeImage(mat: Mat): Array[Byte] = {
      log.info("Starting image encoding")
      val matOfByte = new MatOfByte()
      Imgcodecs.imencode(".png", mat, matOfByte)
      val bytes = matOfByte.toArray
      log.info(s"Image encoding finished. MatOfByte size: ${
         matOfByte.size()
      }; Bytes length: ${
         bytes.length
      }")
      matOfByte.release()
      mat.release()
      bytes
   }

}
