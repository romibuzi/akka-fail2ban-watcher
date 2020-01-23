package com.romibuzi.fail2banwatcher

import slick.jdbc.SQLiteProfile
import slick.jdbc.SQLiteProfile.api._
import slick.lifted.{ProvenShape, Tag}

import scala.concurrent.{ExecutionContext, Future}

class Bans(tag: Tag) extends Table[(String, String, Int)](tag, "bans") {
  def jail: Rep[String]                      = column[String]("jail")
  def ip: Rep[String]                        = column[String]("ip")
  def timeofban: Rep[Int]                    = column[Int]("timeofban")
  def * : ProvenShape[(String, String, Int)] = (jail, ip, timeofban)
}

object Bans {
  def getBannedIPs(implicit db: SQLiteProfile.backend.Database, ec: ExecutionContext): Future[Seq[BannedIP]] = {
    val bans = TableQuery[Bans]

    val query = bans
      .groupBy(_.ip)
      .map { case (ip, results) =>
        ip -> results.length
      }
      .result
      .collect { bansCountPerIP =>
        bansCountPerIP.map { case (ip, bansCount) =>
          BannedIP(ip, bansCount)
        }
      }

    db.run(query)
  }
}
