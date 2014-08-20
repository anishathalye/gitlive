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
    while (true) {
      val msg = queue.take()
      channel.basicPublish(EXCHANGE_NAME, "", null, msg.getBytes)
    }
  }

  private var channel: Channel = null

  private val EXCHANGE_NAME = "gh-events"

  private val HOST = "localhost"

}
