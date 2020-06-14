package com.romibuzi.fail2banwatcher

import java.io.IOException

import zio._

import scala.annotation.tailrec
import scala.io.{BufferedSource, Source}

case class IPRange(start: Long, end: Long, country: Country)

final class IP2LocationGeoIP(val ranges: Array[IPRange]) extends GeoIP.Service {
  private def findIPRangeOfIP(targetIp: Long): Option[IPRange] = {
    @tailrec
    def binarySearch(start: Int, end: Int): Option[IPRange] = {
      if (start > end) return None

      val middle = (start + end) / 2
      if (ranges(middle).start <= targetIp && targetIp <= ranges(middle).end) {
        Some(ranges(middle))
      } else if (ranges(middle).start > targetIp) {
        binarySearch(start, middle - 1)
      } else {
        binarySearch(middle + 1, end)
      }
    }

    binarySearch(0, ranges.length - 1)
  }

  def findCountryOfIP(targetIP: Long): Option[Country] =
    findIPRangeOfIP(targetIP).map(range => range.country)
}

object IP2LocationGeoIP {
  val live: ZLayer[Has[String], IOException, GeoIP] = {
    ZLayer.fromServiceManaged { ip2locationDbResourcePath =>
      ZManaged
        .makeEffect(readResource(ip2locationDbResourcePath))(source => ZIO.effectTotal(source.close()))
        .mapEffect(_.getLines())
        .map(parseLines)
        .map(ranges => new IP2LocationGeoIP(ranges))
        .mapError(_ => new IOException(s"Could not load ip2location database from `$ip2locationDbResourcePath` resource file"))
    }
  }

  def readResource(path: String): BufferedSource =
    Source.fromResource(path)

  def parseLines(lines: Iterator[String]): Array[IPRange] = {
    lines.flatMap { line =>
      line match {
        case s"$start,$end,$countryCode,$countryName" =>
          Some(
            IPRange(start.toLong, end.toLong, Country(countryCode, countryName))
          )
        case _ => None
      }
    }.toArray
  }
}
