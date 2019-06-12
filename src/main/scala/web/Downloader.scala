package web

import java.util.concurrent.ThreadPoolExecutor

import akka.actor.Actor

import scala.concurrent.{ExecutionContext, Future}
import scala.io.Source

object Downloader {

}

class Downloader extends Actor {

   implicit val ec = ExecutionContext.fromExecutor(new ThreadPoolExecutor())

   def downloadToByteArray(url: String): Future[Array[Byte]] = Future {
      Source.fromURL(url).toArray
   }

   override def receive = {
      case _ =>
   }
}
