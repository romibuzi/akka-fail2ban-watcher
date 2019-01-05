package com.romibuzi.fail2banwatcher

import akka.NotUsed
import akka.stream.alpakka.slick.scaladsl.{Slick, SlickSession}
import akka.stream.scaladsl.{Sink, Source}

import scala.concurrent.Future

class Watcher {
  def scanBannedIPs(implicit session: SlickSession): Source[String, NotUsed] = {
    import session.profile.api._
    Slick.source(sql"SELECT ip FROM bans;".as[String])
  }

  def countBansPerIP: Sink[String, Future[Map[String, Int]]] = {
    Sink.fold[Map[String, Int], String](Map.empty[String, Int]) { (acc, ip) =>
      acc.updated(ip, acc.getOrElse(ip, 0) + 1)
    }
  }
}
