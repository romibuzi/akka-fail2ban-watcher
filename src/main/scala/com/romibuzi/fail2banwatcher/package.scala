package com.romibuzi

import zio.{Has, Task}

package object fail2banwatcher {
  case class Country(code: String, name: String)
  case class BansCountPerCountry(country: Country, bansCount: Long)

  trait BannedIP {
    def ip: String
    def bansCount: Long
  }
  case class UnlocatedBannedIP(ip: String, bansCount: Long) extends BannedIP
  case class LocatedBannedIP(ip: String, bansCount: Long, country: Country) extends BannedIP

  type BansRepository = Has[BansRepository.Service]
  object BansRepository {
    trait Service {
      def getBannedIPs: Task[Seq[UnlocatedBannedIP]]
      def getTopBannedCountries(bannedIPs: Seq[LocatedBannedIP], limit: Int): Seq[BansCountPerCountry]
      def getTopBannedIPs(bannedIPs: Seq[BannedIP], limit: Int): Seq[BannedIP]
    }
  }

  type GeoIP = Has[GeoIP.Service]
  object GeoIP {
    trait Service {
      def findCountryOfIP(targetIP: Long): Option[Country]
    }
  }
}
