import collection.mutable.{Set => MSet}

import com.rabbitmq.client._

import morph.ast._, DSL._, Implicits._
import morph.parser._

object Counter {

  def main(args: Array[String]) {
    val factory = new ConnectionFactory()
    factory.setHost("localhost")
    val connection = factory.newConnection()
    val channel = connection.createChannel()

    channel.exchangeDeclare(EXCHANGE_NAME, "fanout")
    val queueName = channel.queueDeclare().getQueue()
    channel.queueBind(queueName, EXCHANGE_NAME, "")

    val consumer = new QueueingConsumer(channel)
    channel.basicConsume(queueName, true, consumer)

    val locs = MSet[String]()

    val startTime = System.nanoTime
    var messages = 0

    while (true) {
      val delivery = consumer.nextDelivery()
      val message = new String(delivery.getBody())
      val json = JsonParser(message)
      val loc = (message ~> "location").asString
      locs += loc
      messages += 1
      val time = (System.nanoTime - startTime) / 1000000000
      println(s"${locs.size} unique, ${messages} total in ${time} seconds")
    }
  }

  val EXCHANGE_NAME = "gh-events"

}
