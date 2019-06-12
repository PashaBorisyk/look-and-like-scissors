import java.util

import image.ImageProcessor
import org.opencv.core._
import org.opencv.imgcodecs.Imgcodecs

object Main {

   def main(args: Array[String]): Unit = {
      loadOpenCV_Lib()

      val t1 = System.currentTimeMillis()
      val src = Imgcodecs.imread("C:\\pashaborisyk\\projects\\Scala\\look-and-like-scissors\\7647851420_6_1_1.jpg")
      val result = new Mat()
      ImageProcessor.processFilter(src, result)

      //      val result = getGradient2(src)
      //      Imgcodecs.imwrite("./save.png", result)
   }



   @throws[Exception]
   def loadOpenCV_Lib() = { // get the model
      val model = System.getProperty("sun.arch.data.model")
      // the path the .dll lib location
      var libraryPath = "C:/Users/pavel.borissiouk/Downloads/opencv/build/java/x86"
      // check for if system is 64 or 32
      if (model == "64") libraryPath = "C:/Users/pavel.borissiouk/Downloads/opencv/build/java/x64"
      // set the path
      System.setProperty("java.library.path", libraryPath)
      val sysPath = classOf[ClassLoader].getDeclaredField("sys_paths")
      sysPath.setAccessible(true)
      sysPath.set(null, null)
      // load the lib
      System.loadLibrary(Core.NATIVE_LIBRARY_NAME)
   }
}
