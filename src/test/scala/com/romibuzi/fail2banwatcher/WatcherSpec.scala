package com.romibuzi.fail2banwatcher

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.alpakka.slick.javadsl.SlickSession
import akka.stream.scaladsl.{Sink, Source}
import com.romibuzi.fail2banwatcher.App.APP_NAME
import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}

class WatcherSpec extends FlatSpec with Matchers {
  implicit val sys: ActorSystem = ActorSystem(APP_NAME + "test-watcher")
  implicit val mat: ActorMaterializer = ActorMaterializer.create(sys)
  implicit val exc: ExecutionContext = sys.dispatcher
  val config: Config = ConfigFactory.load()

  "Scan Banned IPs" should "list banned ips" in {
    // Given
    implicit val session: SlickSession = SlickSession.forConfig(config)
    val source = new Watcher().scanBannedIPs

    // When
    val result = Await.result(source.runWith(Sink.seq), 1.second)

    // Then
    result should contain theSameElementsAs List("81.151.82.119", "189.115.221.77", "63.142.101.182")
  }

  "Count Bans per IP" should "count the number of times each IP was banned" in {
    // Given
    val counter = new Watcher().countBansPerIP
    val ips = Source(List(
      "81.151.82.119",
      "189.115.221.77", "189.115.221.77",
      "63.142.101.182", "63.142.101.182", "63.142.101.182",
    ))
    val expected = Map("81.151.82.119" -> 1, "189.115.221.77" -> 2, "63.142.101.182" -> 3)

    // When
    val result = Await.result(ips.runWith(counter), 1.second)

    // Then
    result should contain theSameElementsAs expected
  }
}
