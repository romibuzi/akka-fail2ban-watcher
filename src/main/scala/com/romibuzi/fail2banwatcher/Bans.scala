package com.romibuzi.fail2banwatcher

import slick.jdbc.SQLiteProfile
import slick.jdbc.SQLiteProfile.api._
import slick.lifted.{ProvenShape, Tag}
import zio.{UIO, ZIO}

import scala.concurrent.{ExecutionContext, Future}

class Bans(tag: Tag) extends Table[(String, String, Int)](tag, "bans") {
  def jail: Rep[String]                      = column[String]("jail")
  def ip: Rep[String]                        = column[String]("ip")
  def timeofban: Rep[Int]                    = column[Int]("timeofban")
  def * : ProvenShape[(String, String, Int)] = (jail, ip, timeofban)
}

object Bans {
  def getBannedIPs(implicit db: SQLiteProfile.backend.Database, ec: ExecutionContext): Future[Seq[UnlocatedBannedIP]] = {
    val bans = TableQuery[Bans]

    val query = bans
      .groupBy(_.ip)
      .map { case (ip, results) =>
        ip -> results.length
      }
      .result
      .collect { bansCountPerIP =>
        bansCountPerIP.map { case (ip, bansCount) =>
          UnlocatedBannedIP(ip, bansCount)
        }
      }

    db.run(query)
  }

  def getTopBannedCountries(bannedIPs: Seq[LocatedBannedIP], limit: Int): UIO[Seq[BansCountPerCountry]] = {
    ZIO.succeed(
      bannedIPs
        .map(_.country.name)
        .groupMapReduce(identity)(_ => 1)(_ + _)
        .map(BansCountPerCountry.tupled(_))
        .toSeq
        .sortBy(_.bansCount)
        .takeRight(limit)
    )
  }

  def getTopBannedIPs(bannedIPs: Seq[BannedIP], limit: Int): UIO[Seq[BannedIP]] = {
    ZIO.succeed(
      bannedIPs
        .sortBy(_.bansCount)
        .takeRight(limit)
    )
  }
}
