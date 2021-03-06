package slurp

import java.util.concurrent.{ BlockingQueue, LinkedBlockingQueue }

import com.typesafe.config.ConfigFactory

import dispatch._, Defaults._

import morph.ast._, DSL._, Implicits._

object Slurp {

  def main(args: Array[String]) {
    val config = ConfigFactory.load()
    val clientId = config.getString("github.clientId")
    val clientSecret = config.getString("github.clientSecret")
    val ghs = new GitHubStream(clientId, clientSecret)
    val queue = new LinkedBlockingQueue[String]()
    val qd = new QueueDumper(queue)
    qd.start()
    mainLoop(ghs, queue)
  }

  def mainLoop(ghs: GitHubStream, queue: BlockingQueue[String]) {
    while (true) {
      try {
        val (events, pollInterval) = ghs.getEvents()
        val timeStep = pollInterval * 1000
        if (events.nonEmpty) {
          events foreach { event =>
            queue.offer(event)
            Thread.sleep(timeStep)
          }
        } else {
          Thread.sleep(EMPTY_SLEEP)
        }
      } catch {
        case e: Exception => {
          e.printStackTrace()
          Thread.sleep(EMPTY_SLEEP)
        }
      }
    }
  }

  private val EMPTY_SLEEP = 5000

}
