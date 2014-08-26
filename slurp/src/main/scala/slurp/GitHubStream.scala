package slurp

import scala.async.Async.{ async, await }
import scala.concurrent.Await
import scala.concurrent.duration._

import java.util.concurrent.{ ExecutionException, TimeoutException }

import dispatch._, Defaults._

import com.twitter.util.LruMap

import morph.ast._, DSL._, Implicits._
import morph.parser._

final class GitHubStream(clientId: String, clientSecret: String) {

  private var lastPollMillis: Long = 0
  private var pollIntervalMillis: Long = 0
  private var eTag: String = ""
  private var lastId = BigInt("-1")

  def getEvents(): (List[String], Long) = {
    val (events, pollInterval) = getRawEvents()
    val filtered = events filter { event =>
      val fln = event lift "fromLocation" match {
        case Some(loc) => loc != ""
        case None      => false
      }
      val tln = event lift "toLocation" match {
        case Some(loc) => loc != ""
        case None      => false
      }
      fln && tln
    } map { event =>
      ObjectNode(event mapValues { value =>
        StringNode(value)
      }).toString
    }
    (filtered, pollInterval)
  }

  def getRawEvents(): (List[Map[String, String]], Long) = {
    try {
      val headers = Map("If-None-Match" -> eTag)
      val url = apiBase / "events" <<? keys <:< (headers ++ userAgent)
      val req = Http(url OK identity)

      val sleepTimeMillis = lastPollMillis + pollIntervalMillis - System.currentTimeMillis
      if (sleepTimeMillis > 0) {
        Thread.sleep(sleepTimeMillis)
      }

      val resp = Await.result(req, DEFAULT_TIMEOUT)

      lastPollMillis = System.currentTimeMillis
      val pollInterval = (resp getHeader "X-Poll-Interval").toLong / 16 // speed up
      pollIntervalMillis = pollInterval * 1000
      eTag = resp getHeader "ETag" // update for next request

      val json = JsonParser(resp.getResponseBody).asList
      val nextId = if (json.nonEmpty) {
        BigInt((json.head ~> "id").asString)
      } else {
        lastId
      }
      val latest = json filter { event =>
        BigInt((event ~> "id").asString) > lastId
      }
      lastId = nextId

      val filtered = latest filter { event =>
        (event ~> "type").asString match {
          case "ForkEvent" => true
          case "WatchEvent" => true
          case "PullRequestEvent" => (event ~> "payload" ~> "action").asString == "opened"
          case "IssuesEvent" => (event ~> "payload" ~> "action").asString == "opened"
          case _ => false
        }
      }

      val events = filtered map { event =>
        val eventType = (event ~> "type").asString
        val login = (event ~> "actor" ~> "login").asString
        val (targetLogin, targetRest) = (event ~> "repo" ~> "name").asString span { _ != '/' }
        val targetRepo = targetRest.tail
        async {
          val fromLocation = await(getUserLocation(login))
          val toLocation = await(getUserLocation(targetLogin))
          Map(
            "type" -> eventType,
            "fromLogin" -> login,
            "fromLocation" -> fromLocation,
            "toRepo" -> targetRepo,
            "toLogin" -> targetLogin,
            "toLocation" -> toLocation
          )
        }
      }
      (Await.result(Future.sequence(events), pollInterval.seconds), pollInterval)
    } catch {
      case e @ (_: ExecutionException | _: TimeoutException) => {
        e.printStackTrace()
        (List(Map()), 0)
      }
    }
  }

  private val LRU_MAP_SIZE = 20000
  private val locations: LruMap[String, String] = new LruMap[String, String](LRU_MAP_SIZE)

  def getUserLocation(user: String): Future[String] = {
    val location = locations.synchronized { locations lift user }
    location match {
      case Some(loc) => Future(loc)
      case None => {
        val url = apiBase / "users" / user <<? keys <:< userAgent
        val req = Http(url OK as.String)
        req map { res =>
          val loc = JsonParser(res) ~> "location" collect {
            case StringNode(l) => l
          } getOrElse ""
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
  private val userAgent = Map("User-Agent" -> "anishathalye")

  private val DEFAULT_TIMEOUT = 10.seconds


}
