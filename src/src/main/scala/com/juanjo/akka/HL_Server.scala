package com.juanjo.akka

import java.io.File

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.Multipart
import akka.http.scaladsl.model.Multipart.BodyPart
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.FileIO
import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.Future


object HL_Server extends App {
  val testConf: Config = ConfigFactory.parseString("""
    akka.loglevel = INFO
    akka.log-dead-letters = off
    akka.remote.netty.connection-timeout=60s
    akka.http.server.parsing.max-content-length=infinite""")

  implicit val system = ActorSystem("my-system",testConf)
  implicit val materializer = ActorMaterializer()
  implicit val ec = system.dispatcher

  // how to manage a multipart file
  val route =
    path("hello") {
      println("Recieved hello msg")
      get {
        complete {
          "hello akka-http!!!"
        }
      }
    }~
    path("upload") {
      entity(as[Multipart.FormData]) {
        formData =>
        // collect all parts of the multipart as it arrives into a map
        val allPartsF: Future[Map[String, Any]] = formData.parts.mapAsync[(String, Any)](1) {

          case b: BodyPart if b.name == "test" =>
            // stream into a file as the chunks of it arrives and return a future
            // file to where it got stored
            val file = File.createTempFile("jdbc", ".tar.gz", null)
            println("File uploaded")
            b.entity.dataBytes.runWith(FileIO.toFile(file)).map(_ =>
              (b.name -> file))

//          case b: BodyPart =>
//            // collect form field values
//            b.toStrict(2.seconds).map(
//              strict => (b.name -> strict.entity.data.utf8String))

        }.runFold(Map.empty[String, Any])((map, tuple) => map + tuple)

        val done = allPartsF.map { allParts =>
          println("Recieved file")
        }

        // when processing have finished create a response for the user
        onSuccess(allPartsF) { allParts =>
          complete {
            "ok!"
          }
        }
      }
    }~
    complete("Welcome to Akka-HTTP")




  val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)

  println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
  scala.io.StdIn.readLine()
  bindingFuture
    .flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete(_ ⇒ system.shutdown()) // and shutdown when done
}