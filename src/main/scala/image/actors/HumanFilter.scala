package image.actors

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import org.mongodb.scala.bson.collection.immutable.Document
import org.opencv.core._
import org.opencv.dnn.Dnn
import org.opencv.imgproc.Imgproc

object HumanFilter {

   private final val PROTO_TXT = "./resources/prototxt.txt"
   private final val MODEL = "./resources/model.caffemodel"
   private final val THRESHOLD = 0.2
   private final val NET = Dnn.readNetFromCaffe(PROTO_TXT, MODEL)

   private final val CLASSES = Array("background",
      "aeroplane", "bicycle",
      "bird", "boat",
      "bottle", "bus",
      "car", "cat",
      "chair", "cow",
      "diningtable", "dog",
      "horse", "motorbike",
      "person", "pottedplant",
      "sheep", "sofa",
      "train", "tvmonitor"
   )

   final case class DetectHumans(document: Document, images: Array[(String, Mat)])

   def props(backgroundRemoverActor: ActorRef) = Props(new HumanFilter(backgroundRemoverActor))
}

class HumanFilter(backgroundRemoverActor: ActorRef) extends Actor with ActorLogging {

   override def receive = {
      case HumanFilter.DetectHumans(document, images) =>
         filterHavingNoHumans(images(0)._2).foreach { mat =>
            backgroundRemoverActor ! BackgroundRemover.ProcessFilter(document, mat)
         }
      case t => throw new RuntimeException(s"Unknown message type : $t")
   }

   private def filterHavingNoHumans(image: Mat): Option[Mat] = {

      val dst = new Mat()
      Imgproc.resize(image, dst, new Size(300, 300))
      val blob = Dnn.blobFromImage(dst, 0.007843, new Size(300, 300), new Scalar(127.5, 127.5, 127.5, 127.5), false, false)

      HumanFilter.NET.setInput(blob)
      var detection = HumanFilter.NET.forward()
      detection = detection.reshape(1, (detection.total() / 7).toInt)
      val rows = detection.rows()
      if (rows == -1) {
         return null
      }
      val labelConfidence = collection.mutable.Map[String, Double]()
      for (i <- 0 until rows) {
         val confidence = detection.get(i, 2)(0)
         if (confidence > HumanFilter.THRESHOLD) {
            val classID = detection.get(i, 1)(0).toInt
            labelConfidence += HumanFilter.CLASSES(classID) -> confidence
            //            val left = detection.get(i, 3)(0) * image.cols()
            //            val top = detection.get(i, 4)(0) * image.rows()
            //            val right = detection.get(i, 5)(0) * image.cols()
            //            val bottom = detection.get(i, 6)(0) * image.rows()
            //
            //            Imgproc.rectangle(image, new Point(left, top), new Point(right, bottom), new Scalar(0, 255, 0))
            //            val label = HumanFilter.CLASSES(classID) + " " + confidence
            //            val baseLine = new Array[Int](1)
            //            val labelSize = Imgproc.getTextSize(label, Imgproc.FONT_HERSHEY_COMPLEX, 0.5, 1, baseLine)
            //
            //            Imgproc.rectangle(image, new Point(left, top - labelSize.height), new Point(left + labelSize.width, top +
            //               baseLine(0)), new Scalar(255, 255, 255), Imgproc.FILLED)
            //            Imgproc.putText(image, label, new Point(left, top + 20), Imgproc.FONT_HERSHEY_SIMPLEX, 0.5, new Scalar(0,
            //               0, 0))
         }
      }
      detection.release()
      if (!(labelConfidence.contains("person") && labelConfidence("person") > 0.99)) {
         Some(image)
      } else{
         image.release()
         None
      }
   }

}
