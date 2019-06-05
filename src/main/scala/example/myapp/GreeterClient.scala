package example.myapp.helloworld

import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.grpc.GrpcClientSettings
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import example.myapp.helloworld.grpc._

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

object GreeterClient {

  def main(args: Array[String]): Unit = {

    implicit val sys = ActorSystem("HelloWorldClient")
    implicit val mat = ActorMaterializer()
    implicit val ec  = sys.dispatcher

    // println("name")
    // println(GreeterService.name)
    // val clientSettings = GrpcClientSettings.fromConfig(GreeterService.name)
    // println("conf")
    // println(clientSettings)

    val clientSettings = GrpcClientSettings
      .connectToServiceAt("127.0.0.1", 9090)
      .withDeadline(15.second)
      .withTls(false)

    val client: GreeterService = GreeterServiceClient(clientSettings)

    runSingleRequestReplyExample()
    runStreamingRequestExample()
    runStreamingReplyExample()
    runStreamingRequestReplyExample()

    sys.scheduler.schedule(1.second, 1.second) {
      runSingleRequestReplyExample()
    }

    def runSingleRequestReplyExample(): Unit = {
      sys.log.info("Performing request")
      val reply = client.sayHello(HelloRequest("Alice"))
      reply.onComplete {
        case Success(msg) =>
          println(s"got single reply: $msg")
        case Failure(e) =>
          println(s"Error sayHello: $e")
      }
    }

    def runStreamingRequestExample(): Unit = {
      val requests = List("Alice", "Bob", "Peter").map(HelloRequest.apply)
      val reply    = client.itKeepsTalking(Source(requests))
      reply.onComplete {
        case Success(msg) =>
          println(s"got single reply for streaming requests: $msg")
        case Failure(e) =>
          println(s"Error steramingRequest: $e")
      }
    }

    def runStreamingReplyExample(): Unit = {
      val responseStream = client.itKeepsReplying(HelloRequest("Alice"))
      val done: Future[Done] =
        responseStream.runForeach(reply => println(s"got streaming reply: ${reply.message}"))

      done.onComplete {
        case Success(_) =>
          println("stremaingReply done")
        case Failure(e) =>
          println(s"Error streamingReply: $e")
      }
    }

    def runStreamingRequestReplyExample(): Unit = {
      val requestStream: Source[HelloRequest, NotUsed] =
        Source
          .tick(100.millis, 1.second, "tick")
          .zipWithIndex
          .map { case (_, i) => i }
          .map(i => HelloRequest(s"Alice-$i"))
          .take(10)
          .mapMaterializedValue(_ => NotUsed)

      val responseStream: Source[HelloReply, NotUsed] = client.streamHellos(requestStream)
      val done: Future[Done] =
        responseStream.runForeach(reply => println(s"got streaming reply: ${reply.message}"))

      done.onComplete {
        case Success(_) =>
          println("streamingRequestReply done")
        case Failure(e) =>
          println(s"Error streamingRequestReply: $e")
      }
    }
  }
}
