package image

import akka.actor.Actor
import org.opencv.core.{Mat, MatOfByte}
import org.opencv.imgcodecs.Imgcodecs

class ImageLoader extends Actor{

   def createMatFromBytes(bytes:Array[Byte]): Mat = {
      Imgcodecs.imdecode(new MatOfByte(bytes:_*),Imgcodecs.IMREAD_UNCHANGED)
   }

   override def receive = {
      case _ =>
   }
}
