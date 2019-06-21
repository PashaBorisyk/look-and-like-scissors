package actors.queue

import actors.mongo.MongoClientConnection
import actors.web.ImageDownloader
import akka.actor.ActorSystem
import akka.kafka.scaladsl.Consumer.DrainingControl
import akka.kafka.scaladsl.{Committer, Consumer}
import akka.kafka.{CommitterSettings, ConsumerSettings, Subscriptions}
import akka.stream.scaladsl.Keep
import akka.stream.{ActorMaterializer, Materializer}
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.mongodb.scala.bson.collection.immutable.Document

import scala.concurrent.{ExecutionContext, Future, Promise}

object KafkaConsumer{

   def apply(actorSystem: ActorSystem): KafkaConsumer = new KafkaConsumer(actorSystem)

}

class KafkaConsumer(actorSystem: ActorSystem) {

   private val committerSettings = CommitterSettings(actorSystem)
   private implicit val m: Materializer = ActorMaterializer.create(actorSystem)
   private implicit val ec: ExecutionContext = actorSystem.dispatcher

   def startConsuming(onDocumentFound : Document => Unit) = {
      println("Starting consuming messages from queue")

      val consumer = ConsumerSettings(actorSystem, new StringDeserializer(), new StringDeserializer())
         .withBootstrapServers("localhost:9092")
         .withGroupId("c_group_name")
         .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

      Consumer
         .committableSource(consumer, Subscriptions.topics("inserted_documents_ids"))
         .mapAsync(10) { msg =>
            val promise = Promise[Document]
            MongoClientConnection
               .getByID(msg.record.value(), promise)
               .map(doc => Future.successful(onDocumentFound(doc)))
               .map(_ => msg.committableOffset)
         }
         .toMat(Committer.sink(committerSettings))(Keep.both)
         .mapMaterializedValue(DrainingControl.apply)

   }


}
