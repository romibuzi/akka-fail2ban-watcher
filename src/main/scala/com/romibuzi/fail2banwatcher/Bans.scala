package com.romibuzi.fail2banwatcher

import slick.interop.zio.DatabaseProvider
import slick.interop.zio.syntax._
import slick.jdbc.SQLiteProfile.api._
import slick.lifted.{ProvenShape, Tag}
import zio.{Task, ZIO, ZLayer}

object BansTable {
  class Bans(tag: Tag) extends Table[(String, String, Long)](tag, "bans") {
    def jail: Rep[String]                       = column[String]("jail")
    def ip: Rep[String]                         = column[String]("ip")
    def timeofban: Rep[Long]                    = column[Long]("timeofban")
    def * : ProvenShape[(String, String, Long)] = (jail, ip, timeofban)
  }

  val table = TableQuery[BansTable.Bans]
}

final class SlickBansRepository(db: DatabaseProvider) extends BansRepository.Service {
  def getBannedIPs: Task[Seq[UnlocatedBannedIP]] = {
    val query = BansTable.table
      .groupBy(_.ip)
      .map { case (ip, results) =>
        ip -> results.length
      }
      .result

    ZIO.fromDBIO(query).map(bansCountPerIP => bansCountPerIP.map {
      case (ip, bansCount) => UnlocatedBannedIP(ip, bansCount)
    }).provide(db)
  }

  def getTopBannedCountries(bannedIPs: Seq[LocatedBannedIP], limit: Int): Seq[BansCountPerCountry] = {
    bannedIPs
      .groupMapReduce(_.country)(_.bansCount)(_ + _)
      .map(BansCountPerCountry.tupled)
      .toSeq
      .sortWith(_.bansCount > _.bansCount)
      .take(limit)
  }

  def getTopBannedIPs(bannedIPs: Seq[BannedIP], limit: Int): Seq[BannedIP] = {
    bannedIPs
      .sortWith(_.bansCount > _.bansCount)
      .take(limit)
  }
}

object SlickBansRepository {
  val live: ZLayer[DatabaseProvider, Throwable, BansRepository] =
    ZLayer.fromFunctionM { db =>
      ZIO.succeed(new SlickBansRepository(db))
    }
}
