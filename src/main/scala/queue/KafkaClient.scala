package queue

import java.time.Duration

import akka.actor.{Actor, ActorLogging, Props}
import akka.kafka.scaladsl.{Consumer, Producer}
import akka.kafka.{ConsumerSettings, ProducerSettings, Subscriptions}
import akka.stream.scaladsl.{Flow, GraphDSL, RunnableGraph, Sink, Source}
import akka.stream.{ActorMaterializer, ClosedShape, OverflowStrategy}
import io.reactivex.BackpressureStrategy
import io.reactivex.subjects.PublishSubject
import mongo.MongoClientConnection
import org.apache.kafka.clients.consumer.{ConsumerConfig, ConsumerRecord}
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, StringDeserializer, StringSerializer}
import org.mongodb.scala.bson.collection.immutable.Document
import queue.KafkaClient.Publish

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object KafkaClient {

   final case class Publish(value: Any)

   def props(onDocumentFound: Document => Unit) = Props(new KafkaClient(onDocumentFound))
}

class KafkaClient(onDocumentFound: Document => Unit) extends Actor with ActorLogging {

   private final val publisher = PublishSubject.create[Any]()

   implicit private final val actorMaterializer: ActorMaterializer = ActorMaterializer()
   implicit private final val ec: ExecutionContext = context.dispatcher

   private final val bootstrapServers = "localhost:9092"
   private final val consumerTopic = "inserted_documents_ids"
   private final val producerTopic = "ready_to_index_id"
   private final val group =  "akka_streams_group"
   private final val partition = 0
   private final val subscription = Subscriptions.assignment(new TopicPartition(consumerTopic, partition))

   private final val consumerSettings = ConsumerSettings(context.system, new ByteArrayDeserializer, new StringDeserializer)
      .withBootstrapServers(bootstrapServers)
      .withGroupId(group)
      .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
      .withCommitRefreshInterval(Duration.ofSeconds(2))

   private final val producerSettings =
      ProducerSettings(context.system, new StringSerializer, new StringSerializer)
         .withBootstrapServers(bootstrapServers)

   log.info("Constructing kafka producer")
   private final val producer = Source.fromPublisher(publisher.toFlowable(BackpressureStrategy.BUFFER))
      .map(_.toString)
      .map(value => new ProducerRecord[String, String](producerTopic, value))
      .runWith(Producer.plainSink(producerSettings))
   log.info("Constructing kafka producer done")

   log.info("Constructing kafka consumer")
   private final val graph = RunnableGraph.fromGraph(GraphDSL.create() { implicit builder =>
      import akka.stream.scaladsl.GraphDSL.Implicits._

      val kafkaSource = Consumer.plainSource(consumerSettings, subscription)
      val callSink = Sink.foreach[Future[Document]](_.onComplete {
         case Failure(exception) =>
            exception.printStackTrace()
         case Success(value) =>
            onDocumentFound(value)
      })

      val mapFromConsumerRecord = Flow[ConsumerRecord[Array[Byte], String]].map { record =>
         val value = record
            .value()
            .replace("ObjectID(", "")
            .replace(")", "")
            .replace("\"", "")

         MongoClientConnection.getByIDString(value)
      }

      kafkaSource ~> mapFromConsumerRecord ~> callSink

      ClosedShape
   }).run()
   log.info("Constructing kafka consumer done")


   override def receive = {
      case Publish(value) => publisher.onNext(value)
      case t => throw new RuntimeException(s"Unknown message type: $t")
   }

}
