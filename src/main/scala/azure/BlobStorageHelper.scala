package azure

import java.net.URL
import java.nio.ByteBuffer
import java.util.Locale

import com.microsoft.azure.storage.blob.{PipelineOptions, ServiceURL, SharedKeyCredentials, StorageURL}
import io.reactivex.Flowable
import util.Util

object BlobStorageHelper {

   private final val accountName = ""
   private final val accountKey = ""

   private final val credential = new SharedKeyCredentials(accountName, accountKey)
   private final val pipeLine = StorageURL.createPipeline(credential, new PipelineOptions)
   private final val url = new URL(String.format(Locale.ROOT, "https://%s.blob.core.windows.net", accountName))
   private final val serviceURL = new ServiceURL(url, pipeLine)
   private final val containerURL = serviceURL.createContainerURL("images")

   def upload(source: Array[Byte]) = {
      val fileName = Util.generateFileName("png")
      containerURL.createBlockBlobURL(fileName)
         .upload(Flowable.just(ByteBuffer.wrap(source)), source.length)
   }

}
