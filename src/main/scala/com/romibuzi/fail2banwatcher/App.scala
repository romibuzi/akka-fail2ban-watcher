package com.romibuzi.fail2banwatcher

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

import akka.actor.{ActorSystem, Terminated}
import akka.stream.ActorMaterializer
import akka.stream.alpakka.slick.javadsl.SlickSession
import com.jakewharton.fliptables.FlipTable
import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.Logger

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

object App {
  val APP_NAME = "akka-fail2ban-watcher"

  val config: Config = ConfigFactory.load()
  val appLogger = Logger(APP_NAME)

  def main(args: Array[String]): Unit = {
    implicit val sys: ActorSystem = ActorSystem(APP_NAME, config)
    implicit val mat: ActorMaterializer = ActorMaterializer.create(sys)
    implicit val exc: ExecutionContext = sys.dispatcher
    implicit val session: SlickSession = SlickSession.forConfig(config)
    appLogger.info("Actor system initialized")

    val startTime = LocalDateTime.now()
    val watcher = new Watcher()

    watcher
      .scanBannedIPs
      .runWith(watcher.countBansPerIP)
      .onComplete {
        case Success(counters) =>
          val headers = Array("IP", "Number of Bans")
          val data = counters.toArray.sortBy(- _._2).take(10).map { case (ip, bansCount) =>
            Array(ip, bansCount.toString)
          }
          println("Number of distinct banned IPs : " + counters.size)
          println("Top banned IPs :")
          println(FlipTable.of(headers, data))

          appLogger.info(s"Stream successful: ${formatDuration(duration(startTime, LocalDateTime.now))}")
          sys.terminate().onComplete(logTermination)(sys.dispatcher)
        case Failure(err) =>
          appLogger.error("Stream terminated with error: ", err)
          sys.terminate().onComplete(logTermination)(sys.dispatcher)
      }
  }

  def logTermination(terminated: Try[Terminated]): Unit = {
    terminated match {
      case Success(_) => appLogger.info("Actor system terminated")
      case Failure(err) => appLogger.error("Error while terminating actors: ", err)
    }
  }

  def duration(startTime: LocalDateTime, endTime: LocalDateTime): (Long, Long, Long, Long) = {
    var tempDT = LocalDateTime.from(startTime)

    val hours = tempDT.until(endTime, ChronoUnit.HOURS)
    tempDT = tempDT.plusHours(hours)

    val minutes = tempDT.until(endTime, ChronoUnit.MINUTES)
    tempDT = tempDT.plusMinutes(minutes)

    val seconds = tempDT.until(endTime, ChronoUnit.SECONDS)
    tempDT = tempDT.plusSeconds(seconds)

    val ms = tempDT.until(endTime, ChronoUnit.MILLIS)

    (hours, minutes, seconds, ms)
  }

  def formatDuration(duration: (Long, Long, Long, Long)): String = {
    val (hours, minutes, seconds, ms) = duration
    s"Elapsed: ${hours}h ${minutes}min $seconds.${ms}s"
  }
}
