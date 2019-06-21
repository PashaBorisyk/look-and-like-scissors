package actors.image

import actors.web.ImageUploader
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import org.mongodb.scala.bson.collection.immutable.Document
import org.opencv.core.{Mat, MatOfByte}
import org.opencv.imgcodecs.Imgcodecs

object ImageCoder {

   def props(): Props = Props(new ImageCoder())

   final case class DecodeImage(documentWithSource: (Document, Array[Byte]))

   final case class EncodeImage(documentWithMat: (Document, Mat))

}

class ImageCoder extends Actor with ActorLogging {

   var uploaderActor: ActorRef = Actor.noSender
   var imageProcessorActor: ActorRef = Actor.noSender

   override def preStart() = {
      imageProcessorActor = context.actorOf(ImageProcessor.props(),"image-processor-actor")
      uploaderActor = context.actorOf(ImageUploader.props(),"image-uploader-actor")
   }

   override def receive = {
      case ImageCoder.DecodeImage((document, source)) =>
         imageProcessorActor ! ImageProcessor.ProcessFilter(document -> decodeImage(source))
      case ImageCoder.EncodeImage((document, image)) =>
         uploaderActor ! ImageUploader.UploadImage(document -> encodeImage(image))

      case _ => throw new RuntimeException("Unknown type of operation")
   }

   private def decodeImage(bytes: Array[Byte]): Mat = {
      log.info("Starting actors.image decoding")
      val image = Imgcodecs.imdecode(new MatOfByte(bytes: _*), Imgcodecs.IMREAD_ANYCOLOR)
      log.info(s"Image decoding finished. Image size: ${image.size()}")
      image
   }

   private def encodeImage(mat: Mat): Array[Byte] = {
      log.info("Starting actors.image encoding")
      val matOfByte = new MatOfByte()
      Imgcodecs.imencode(".png", mat, matOfByte)
      val bytes = matOfByte.toArray
      log.info(s"Image encoding finished. MatOfByte size: ${matOfByte.size()}; Bytes length: ${bytes.length}")
      bytes
   }

}
