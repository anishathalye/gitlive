package slurp

import java.util.concurrent.{BlockingQueue, ArrayBlockingQueue}

import com.typesafe.config.ConfigFactory

import dispatch._, Defaults._

import morph.ast._, DSL._, Implicits._

object Slurp {

  def main(args: Array[String]) {
    val config = ConfigFactory.load()
    val clientId = config.getString("github.clientId")
    val clientSecret = config.getString("github.clientSecret")
    val ghs = new GitHubStream(clientId, clientSecret)
    val queue = new ArrayBlockingQueue[String](QUEUE_CAPACITY)
    val qd = new QueueDumper(queue)
    qd.start()
    mainLoop(ghs, queue)
  }

  def mainLoop(ghs: GitHubStream, queue: BlockingQueue[String]) {
    while (true) {
      val events = ghs.getEvents()
      events foreach { event =>
        queue.offer(event)
      }
    }
  }

  private val QUEUE_CAPACITY = 256

}
