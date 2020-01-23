package com.romibuzi

package object fail2banwatcher {
  case class Country(code: String, name: String)
  case class IPRange(start: Long, end: Long, country: Country)

  case class BannedIP(ip: String, bansCount: Int)
  case class LocatedBannedIP(ip: String, bansCount: Int, country: Country)

  case class BansCountPerCountry(countryName: String, bansCount: Int)
}
