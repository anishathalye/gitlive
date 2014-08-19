package slurp

import scala.async.Async.{ async, await }
import scala.collection.mutable.{ Map => MMap }
import scala.concurrent.Await
import scala.concurrent.duration._

import dispatch._, Defaults._

import morph.ast._, DSL._, Implicits._
import morph.parser._

final class GitHubStream(clientId: String, clientSecret: String) {

  private var lastPollMillis: Long = 0
  private var pollIntervalMillis: Long = 0
  private var eTag: String = ""

  def getEvents(): List[String] = getRawEvents filter { event =>
    event lift "location" match {
      case Some(loc) => loc != ""
      case None => false
    }
  } map { event =>
    ObjectNode(event mapValues { value =>
      StringNode(value)
    }).toString
  }

  def getRawEvents(): List[Map[String, String]] = {
    try {
      val headers = Map("ETag" -> eTag)
      val url = apiBase / "events" <<? keys <:< headers
      val req = Http(url OK identity)

      val sleepTimeMillis = lastPollMillis + pollIntervalMillis - System.currentTimeMillis
      if (sleepTimeMillis > 0) {
        Thread.sleep(sleepTimeMillis)
      }

      val resp = Await.result(req, DEFAULT_TIMEOUT)

      lastPollMillis = System.currentTimeMillis
      val pollInterval = (resp getHeader "X-Poll-Interval").toLong
      pollIntervalMillis = pollInterval * 1000
      eTag = resp getHeader "ETag" // update for next request

      val events = JsonParser(resp.getResponseBody).asList map { event =>
        val eventType = (event ~> "type").asString
        val login = (event ~> "actor" ~> "login").asString
        async {
          val location = await(getUserLocation(login))
          Map(
            "type" -> eventType,
            "login" -> login,
            "location" -> location
          )
        }
      }
      Await.result(Future.sequence(events), pollInterval.seconds)
    } catch {
      case _: Exception => List(Map())
    }
  }

  // TODO replace this with a LRU cache
  private val locations: MMap[String, String] = MMap[String, String]()

  private def getUserLocation(user: String): Future[String] = {
    val location = locations.synchronized { locations lift user }
    location match {
      case Some(loc) => Future(loc)
      case None => {
        val url = apiBase / "users" / user <<? keys
        val req = Http(url OK as.String)
        req map { res =>
          val loc = JsonParser(res) ~> "location" collect {
            case StringNode(l) => l
          } getOrElse ""
          // this is somewhat icky
          locations.synchronized {
            locations(user) = loc
          }
          loc
        }
      }
    }
  }

  private val apiBase = url("https://api.github.com")
  private val keys = Map("client_id" -> clientId, "client_secret" -> clientSecret)

  private val DEFAULT_TIMEOUT = 10.seconds

}
