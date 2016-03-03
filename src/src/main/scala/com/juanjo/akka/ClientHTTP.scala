package com.juanjo.akka
import java.io.File

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.stream.scaladsl.{FileIO, ImplicitMaterializer, Source}
import akka.util.ByteString
import com.typesafe.config.{ConfigFactory, Config}

import scala.concurrent.Future
import scala.util.{Failure, Success}

class ClientHTTP extends Actor
with ImplicitMaterializer
with ActorLogging {

  import akka.pattern.pipe
  import context.dispatcher


  override def preStart() = {

    val http = Http(context.system)

    val r=createRequest("http://localhost:8080/upload", new File("/home/jjlopez/Stratio/workspaceJDBC/JDBCPackage/CrossdataJDBC_v1.2.0-SNAPSHOT.tar.gz"))
    r onComplete {
      case Success(req) => http.singleRequest(req).pipeTo(self)
      case Failure(f) => println("No se ha podido tratar el archivo:" + f.getMessage)
    }

  }

  def createEntity(file: File): Future[RequestEntity] = {
    require(file.exists())
    val fileIO=FileIO.fromFile(file)
    val formData =
      Multipart.FormData(
        Source.single(
          Multipart.FormData.BodyPart(
            "test",
            HttpEntity(ContentTypes.`application/octet-stream`, file.length(), fileIO),
            Map("filename" -> file.getName))))
    Marshal(formData).to[RequestEntity]
  }

  def createRequest(target: Uri, file: File): Future[HttpRequest] =
    for {
      e ← createEntity(file)
    } yield HttpRequest(HttpMethods.POST, uri = target, entity = e)


  def receive = {
    case HttpResponse(StatusCodes.OK, headers, entity, _) =>
      log.info("Got response, body: " + entity.dataBytes.runFold(ByteString(""))(_ ++ _))
    case HttpResponse(code, _, _, _) =>
      log.info("Request failed, response code: " + code)
  }

}
object ClientObject extends App {
  val testConf: Config = ConfigFactory.parseString("""
    akka.loglevel = INFO
    akka.log-dead-letters = off
    akka.remote.netty.connection-timeout=60s""")
  implicit val system = ActorSystem("my-system",testConf)
  val client =system.actorOf(Props(new ClientHTTP), "client")
}

