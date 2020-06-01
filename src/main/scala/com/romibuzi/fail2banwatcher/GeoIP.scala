package com.romibuzi.fail2banwatcher

import zio.{IO, Task, ZIO}

import scala.annotation.tailrec
import scala.io.Source

class GeoIP(val ranges: Array[IPRange]) {
  def findIPRangeOfIP(targetIp: Long): Option[IPRange] = {
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

  def findCountryOfIP(targetIp: Long): IO[Unit, Country] =
    ZIO.fromOption(findIPRangeOfIP(targetIp).map(range => range.country))
}

object GeoIP {
  def readResource(path: String): Task[Iterator[String]] =
    Task.effect(Source.fromResource(path).getLines)

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

  def loadIP2LocationDatabase(databaseResourcePath: String): ZIO[Any, Throwable, GeoIP] = {
    readResource(databaseResourcePath)
      .map(parseLines)
      .map(ranges => new GeoIP(ranges))
  }
}
