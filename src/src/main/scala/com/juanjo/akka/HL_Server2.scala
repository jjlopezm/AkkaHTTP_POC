package com.juanjo.akka

import java.io.File

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.Multipart
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.util.ByteString
import com.typesafe.config.{Config, ConfigFactory}


object HL_Server2 extends App {
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
      val file = File.createTempFile("jdbc", ".tar.gz", null)
      entity(as[Multipart.FormData]) { (formdata: Multipart.FormData) ⇒
        val fileNamesFuture = formdata.parts.mapAsync(1) { p =>

          println(s"Got part. name: ${p.name} filename: ${p.filename}")

          println("Counting size...")
          @volatile var lastReport = System.currentTimeMillis()
          @volatile var lastSize = 0L
          def receiveChunk(counter: (Long, Long), chunk: ByteString): (Long, Long) = {
            val (oldSize, oldChunks) = counter
            val newSize = oldSize + chunk.size
            val newChunks = oldChunks + 1

            val now = System.currentTimeMillis()
            if (now > lastReport + 1000) {
              val lastedTotal = now - lastReport
              val bytesSinceLast = newSize - lastSize
              val speedMBPS = bytesSinceLast.toDouble / 1000000 /* bytes per MB */ / lastedTotal * 1000 /* millis per second */

              println(f"Already got $newChunks%7d chunks with total size $newSize%11d bytes avg chunksize ${newSize / newChunks}%7d bytes/chunk speed: $speedMBPS%6.2f MB/s")

              lastReport = now
              lastSize = newSize
            }
            (newSize, newChunks)
          }

//          p.entity.dataBytes.runWith(FileIO.toFile(file)).map(_ =>
//            (p.name -> file))

          p.entity.dataBytes.runFold((0L, 0L))(receiveChunk).map {
            case (size, numChunks) ⇒
              println(s"Size is $size")
              (p.name, p.filename, size)
          }


        }.runFold(Seq.empty[(String, Option[String], Long)]) {
          case (acc: Seq[(String, Option[String], Long)], current) =>
            acc :+ current
        } map(_.mkString(", "))

        complete {
          fileNamesFuture
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