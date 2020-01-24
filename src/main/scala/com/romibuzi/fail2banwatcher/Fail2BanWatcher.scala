package com.romibuzi.fail2banwatcher

import java.util.concurrent.TimeUnit

import slick.jdbc.SQLiteProfile
import slick.jdbc.SQLiteProfile.api._
import zio.clock._
import zio.console._
import zio.{App, UIO, ZEnv, ZIO}

import scala.Console
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

object Fail2BanWatcher extends App {
  val NUMBER_OF_ELEMENTS_TO_DISPLAY = 10

  def run(args: List[String]): ZIO[ZEnv, Nothing, Int] = {
    implicit val ec: ExecutionContextExecutor = ExecutionContext.global
    implicit val db: SQLiteProfile.backend.Database = Database.forConfig("bans")

    val program = for {
      startTime        <- currentTime(TimeUnit.MILLISECONDS)
      geoIP            <- GeoIP.loadIP2LocationDatabase("ip2location.csv")
      bannedIPs        <- ZIO.fromFuture { implicit ec => Bans.getBannedIPs }
      locatedBannedIPs <- ZIO.foreach(bannedIPs)(findLocationOfIP(geoIP, _))

      topBannedIps <- getTopBannedIPs(locatedBannedIPs)
      _ <- putStrLn(s"${Console.YELLOW}Top banned IPs :${Console.RESET}")
      _ <- ZIO.foreach(topBannedIps)(ip => putStrLn(s"${ip.bansCount} bans : ${ip.ip}"))

      topBannedCountries <- getTopBannedCountries(locatedBannedIPs)
      _ <- putStrLn(s"\n${Console.YELLOW}Top banned countries :${Console.RESET}")
      _ <- ZIO.foreach(topBannedCountries)(country => putStrLn(s"${country.bansCount} bans : ${country.countryName}"))

      endTime <- currentTime(TimeUnit.MILLISECONDS)
      _ <- putStrLn(s"\n${Console.GREEN}Analysis completed in ${(endTime - startTime) / 1000f} seconds")
    } yield ()

    program
      .onError(error => putStrLn(s"${Console.RED} Error happened: ${error.failures.head.getMessage}"))
      .fold(_ => 1, _ => 0)
  }

  def findLocationOfIP(geoIP: GeoIP, bannedIP: BannedIP): UIO[Option[LocatedBannedIP]] = {
    ZIO.succeed(
      for {
        ipAsLong <- IpConverter.ipv4ToLong(bannedIP.ip)
        country  <- geoIP.findCountryOfIP(ipAsLong)
      } yield LocatedBannedIP(bannedIP.ip, bannedIP.bansCount, country)
    )
  }

  def getTopBannedCountries(bannedIPs: List[Option[LocatedBannedIP]]): UIO[Seq[BansCountPerCountry]] = {
    ZIO.succeed(
      bannedIPs
        .flatten
        .map(_.country.name)
        .groupMapReduce(identity)(_ => 1)(_ + _)
        .map(BansCountPerCountry.tupled(_))
        .toSeq
        .sortBy(_.bansCount)
        .takeRight(NUMBER_OF_ELEMENTS_TO_DISPLAY)
    )
  }

  def getTopBannedIPs(bannedIPs: List[Option[LocatedBannedIP]]): UIO[List[LocatedBannedIP]] = {
    ZIO.succeed(
      bannedIPs
        .flatten
        .sortBy(_.bansCount)
        .takeRight(NUMBER_OF_ELEMENTS_TO_DISPLAY)
    )
  }
}
