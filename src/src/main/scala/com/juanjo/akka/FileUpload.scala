package com.juanjo.akka

/**
  * Created by jjlopez on 25/02/16.
  */
class FileUpload {
//  val uploadVideo =
//    path("video") {
//      entity(as[Multipart.FormData]) { formData =>
//
//        // collect all parts of the multipart as it arrives into a map
//        val allPartsF: Future[Map[String, Any]] = formData.parts.mapAsync[(String, Any)](1) {
//
//          case b: BodyPart if b.name == "file" =>
//            // stream into a file as the chunks of it arrives and return a future
//            // file to where it got stored
//            val file = File.createTempFile("upload", "tmp")
//            b.entity.dataBytes.runWith(FileIO.toFile(file)).map(_ =>
//              (b.name -> file))
//
//          case b: BodyPart =>
//            // collect form field values
//            b.toStrict(2.seconds).map(strict =>
//              (b.name -> strict.entity.data.utf8String))
//
//        }.runFold(Map.empty[String, Any])((map, tuple) => map + tuple)
//
//        val done = allPartsF.map { allParts =>
//          // You would have some better validation/unmarshalling here
//          db.create(Video(
//            file = allParts("file").asInstanceOf[File],
//            title = allParts("title").asInstanceOf[String],
//            author = allParts("author").asInstanceOf[String]))
//        }
//
//        // when processing have finished create a response for the user
//        onSuccess(allPartsF) { allParts =>
//          complete {
//            "ok!"
//          }
//        }
//      }

}
