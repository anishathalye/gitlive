package slurp

import java.util.concurrent.BlockingQueue

import com.rabbitmq.client._

class QueueDumper(val queue: BlockingQueue[String]) {

  def createConnection() {
    val factory = new ConnectionFactory();
    factory.setHost("localhost");
    val connection = factory.newConnection();
    channel = connection.createChannel();
    channel.exchangeDeclare(EXCHANGE_NAME, "fanout")
  }

  def start() {
    createConnection()
    val thread = new Thread(new Runnable {
      override def run {
        loop()
      }
    })
    thread.start()
  }

  def loop() {
    timed (POST_DELAY) {
      val msg = queue.take()
      channel.basicPublish(EXCHANGE_NAME, "", null, msg.getBytes)
    }
  }

  def timed(waitTimeMillis: Long)(action: => Unit) {
    while (true) {
      val start = System.currentTimeMillis
      action
      val sleepTime = waitTimeMillis - (System.currentTimeMillis - start)
      if (sleepTime > 0) {
        Thread.sleep(sleepTime)
      }
    }
  }

  private var channel: Channel = null

  private val EXCHANGE_NAME = "gh-events"

  private val HOST = "localhost"

  private val POST_DELAY: Long = 1000

}
