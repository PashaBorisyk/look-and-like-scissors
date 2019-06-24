package image.actors

import java.util

import akka.actor.{Actor, ActorLogging, Props}
import org.mongodb.scala.bson.collection.immutable.Document
import org.opencv.core._
import org.opencv.imgproc.Imgproc

object BackgroundRemover {

   def props() = Props(new BackgroundRemover)

   final case class ProcessFilter(document: Document,image: Mat)

}

class BackgroundRemover() extends Actor with ActorLogging {

   private final val FLOOD_FILL_RELATIVE_SEED_POINT = 0.01
   private final val FLOOD_FILL_TOLERANCE = 2
   private final val LO_DIFF = 2.9
   private final val HI_DIFF = 3.3
   private final val BLUR_FACTOR = 3

   override def receive: Receive = {
      case BackgroundRemover.ProcessFilter(document, mat) =>
         context.parent ! ImageCoder.EncodeImage(document, removeBackground(mat))
      case t => throw new RuntimeException(s"Unknown message type : $t")
   }

   private def removeBackground(source: Mat): Mat = {
      log.info("Starting image processing")
      applyFilter(source)
   }

   private def applyFilter(src: Mat) : Mat = {
      val alphaMask = getGradient(src)
//      return alphaMask
      Imgproc.floodFill(
         alphaMask, new Mat(), new Point(
            (FLOOD_FILL_RELATIVE_SEED_POINT * alphaMask.width()).toInt,
            (FLOOD_FILL_RELATIVE_SEED_POINT * alphaMask.height()).toInt
         ), new Scalar(0),
         new Rect(),
         new Scalar(LO_DIFF),
         new Scalar(HI_DIFF),
         Imgproc.FLOODFILL_FIXED_RANGE
      )
//      //
//            performMorphologyEx(alphaMask, Imgproc.MORPH_ERODE, 1)
//            performMorphologyEx(alphaMask, Imgproc.MORPH_OPEN, 2)
//            performMorphologyEx(alphaMask, Imgproc.MORPH_ERODE, 1)


      Imgproc.threshold(alphaMask, alphaMask, 0, 250, Imgproc.THRESH_BINARY)
      alphaMask.convertTo(alphaMask, CvType.CV_8UC1, 255)

      if (BLUR_FACTOR > 0)
         Imgproc.GaussianBlur(alphaMask, alphaMask, new Size(BLUR_FACTOR, BLUR_FACTOR), BLUR_FACTOR)

      val dst = new Mat()
      addAlphaChannel(src, dst, alphaMask)
      log.info("Image processing finished")
      dst
   }

   private def getGradient(src: Mat): Mat = {

      val preparedSrc = new Mat()
      Imgproc.cvtColor(src, preparedSrc, Imgproc.COLOR_BGR2GRAY)
      val res = new Mat()
      preparedSrc.convertTo(res, CvType.CV_32F)
      val gradX = new Mat()
      Imgproc.Sobel(res, gradX, CvType.CV_32F, 0, 1, 3, 1 / 8.0)
      val gradY = new Mat()
      Imgproc.Sobel(res, gradY, CvType.CV_32F, 1, 0, 3, 1 / 8.0)

      val result = new Mat()
      Core.magnitude(gradX, gradY, result)
      Core.add(result, new Scalar(50.0), result)
      result
   }

   private def getGradientCustom(src: Mat): Mat = {

      val preparedSrc = new Mat()
      Imgproc.cvtColor(src, preparedSrc, Imgproc.COLOR_BGR2GRAY)
      preparedSrc.convertTo(preparedSrc, CvType.CV_32F)
      val gradX = derivative(preparedSrc, 1, 0)
      val gradY = derivative(preparedSrc, 0, 1)
      val magnitude = new Mat()
      Core.magnitude(gradX, gradY, magnitude)
      val result = new Mat()
      Core.add(magnitude, new Scalar(50f), result)
      result
   }

   private def derivative(preparedSrc: Mat, dx: Int, dy: Int): Mat = {

      val resolution = preparedSrc.width() * preparedSrc.height()

      val kernelSize = if (resolution < 1280 * 1280) 3
      else if (resolution < 2000 * 2000) 5
      else if (resolution < 3000 * 3000) 9
      else 15

      val kernelFactor = new Scalar(if (kernelSize == 3) 1 else 2)

      val kernelRows = new Mat()
      val kernelColumns = new Mat()
      Imgproc.getDerivKernels(kernelRows, kernelColumns, dx, dy, kernelSize, true)

      val multipleKernelRows = new Mat()
      Core.multiply(kernelRows, kernelFactor, multipleKernelRows)

      val multipleKernelColumns = new Mat()
      Core.multiply(kernelColumns, kernelFactor, multipleKernelColumns)
      Imgproc.sepFilter2D(preparedSrc, preparedSrc, CvType.CV_32FC1, multipleKernelRows, multipleKernelColumns)
      preparedSrc
   }


   private def addAlphaChannel(src: Mat, dst: Mat, alpha: Mat): Unit = {

      val matList = new util.ArrayList[Mat]()
      Core.split(src, matList)
      val bgra = new util.ArrayList[Mat]()
      bgra.add(matList.get(0))
      bgra.add(matList.get(1))
      bgra.add(matList.get(2))
      bgra.add(alpha)
      Core.merge(bgra, dst)
   }

   private def performMorphologyEx(alphaMask: Mat, operation: Int, iterations: Int): Unit = {
      var elementSize = Math.sqrt(alphaMask.width() * alphaMask.height()) / 30
      if (elementSize < 3)
         elementSize = 3
      if (elementSize > 20)
         elementSize = 20

      val se = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(elementSize, elementSize))
      Imgproc.morphologyEx(alphaMask, alphaMask, operation, se, new Point(1, 1), iterations)
   }

}
