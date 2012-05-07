package phase

import blueeyes.BlueEyesServiceBuilder
import blueeyes.concurrent.Future
import blueeyes.core.http.{HttpStatus, HttpRequest, HttpResponse}
import blueeyes.core.http.MimeTypes._
import blueeyes.core.http.HttpStatusCodes._
import blueeyes.core.data.{ByteChunk, BijectionsChunkString, BijectionsChunkJson}
import blueeyes.json.JsonAST.{JObject, JArray, JValue}
import blueeyes.persistence.mongo.{MongoQueryBuilder}
import blueeyes.persistence.mongo.EnvMongo

import com.mongodb.MongoURI

trait PostService extends BlueEyesServiceBuilder with MongoQueryBuilder with BijectionsChunkJson with BijectionsChunkString {

  val postMongo = service("posts", "0.1") {
    logging { log => context =>
      startup {
        log.debug("starting")
        val mongoURI = new MongoURI("mongodb://127.0.0.1:27017/phase")
        PostConfig(new EnvMongo(mongoURI, context.config.configMap("mongo"))).future
      } ->
      request { config: PostConfig =>
        path("/posts") {
          contentType(application/json) {
            get { request: HttpRequest[JValue] =>
              log.debug("getting posts")
              config.database(selectAll.from("posts")) map { records =>
                HttpResponse[JValue](content = Some(JArray(records.toList)))
              }
            }
          } ~
          contentType(application/json) {
            post { request: HttpRequest[JValue] =>
              request.content map { jv: JValue =>
                config.database(insert(jv --> classOf[JObject]).into("posts"))
                Future.sync(HttpResponse[JValue](content = request.content))
              } getOrElse {
                Future.sync(HttpResponse[JValue](status = HttpStatus(BadRequest)))
              }
            }
          } ~
          produce(text/html) {
            get { request: HttpRequest[ByteChunk] =>
              val content = <html>
                <head>
                  <script defer="defer" src="/jquery-1.7.min.js"></script>
                  <script defer="defer" src=""></script>
                </head>
                <body>
                </body>
              </html>
              Future.sync(HttpResponse[String](content = Some(content.buildString(true))))
            }
          }
        }
      } ->
      shutdown { helloConfig: PostConfig =>
        Future.sync(())
      }
    }
  }
}

case class PostConfig(envMongo: EnvMongo) {
  val database = envMongo.database(envMongo.mongoURI.getDatabase)
}