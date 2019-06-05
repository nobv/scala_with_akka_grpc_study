package example.myapp.helloworld

import akka.actor.ActorSystem
import akka.http.scaladsl.UseHttp2.Always
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.{Http, HttpConnectionContext}
import akka.stream.{ActorMaterializer, Materializer}
import com.typesafe.config.ConfigFactory
import example.myapp.helloworld.grpc._

import scala.concurrent.{ExecutionContext, Future}

object GreeterServer {

  def main(args: Array[String]): Unit = {
    val conf = ConfigFactory
      .parseString("akka.http.server.preview.enable-http2 = on")
      .withFallback(ConfigFactory.defaultApplication())

    val system = ActorSystem("HelloWorld", conf)
    new GreeterServer(system).run()
  }
}

class GreeterServer(system: ActorSystem) {
  def run(): Future[Http.ServerBinding] = {
    implicit val sys: ActorSystem     = system
    implicit val mat: Materializer    = ActorMaterializer()
    implicit val ec: ExecutionContext = sys.dispatcher

    val service: HttpRequest => Future[HttpResponse] =
      GreeterServiceHandler(new GreeterServiceImpl())

    val binding = Http().bindAndHandleAsync(service,
                                            interface = "127.0.0.1",
                                            port = 9090,
                                            connectionContext =
                                              HttpConnectionContext(http2 = Always))

    binding.foreach { binding =>
      println(s"gRPC server bound to: ${binding.localAddress}")
    }

    binding

  }
}
