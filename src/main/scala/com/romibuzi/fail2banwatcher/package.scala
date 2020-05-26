package com.romibuzi

package object fail2banwatcher {
  case class Country(code: String, name: String)
  case class IPRange(start: Long, end: Long, country: Country)

  trait BannedIP {
    def ip: String
    def bansCount: Int
  }
  case class UnlocatedBannedIP(ip: String, bansCount: Int) extends BannedIP
  case class LocatedBannedIP(ip: String, bansCount: Int, country: Country) extends BannedIP

  case class BansCountPerCountry(countryName: String, bansCount: Int)
}
