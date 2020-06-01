package com.romibuzi.fail2banwatcher

import java.util.concurrent.TimeUnit

import slick.jdbc.SQLiteProfile
import slick.jdbc.SQLiteProfile.api._
import zio._
import zio.clock._
import zio.console._

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}
import scala.io.AnsiColor

object Fail2BanWatcher extends App {
  val NUMBER_OF_ELEMENTS_TO_DISPLAY = 10

  def run(args: List[String]): ZIO[ZEnv, Nothing, ExitCode] = {
    implicit val ec: ExecutionContextExecutor = ExecutionContext.global
    implicit val db: SQLiteProfile.backend.Database = Database.forConfig("bans")

    val program = for {
      _         <- putStrLn(s"${AnsiColor.BLUE}Starting analysis${AnsiColor.RESET}")
      startTime <- currentTime(TimeUnit.MILLISECONDS)
      geoIP     <- GeoIP.loadIP2LocationDatabase("ip2location.csv")
      bannedIPs <- ZIO.fromFuture { implicit ec => Bans.getBannedIPs }

      (unlocatedBannedIPs, locatedBannedIPs) <- ZIO.partition(bannedIPs)(findLocationOfIP(geoIP, _))
      _ <- displayUnlocatedIPs(unlocatedBannedIPs)

      topBannedIps <- Bans.getTopBannedIPs(locatedBannedIPs ++ unlocatedBannedIPs, NUMBER_OF_ELEMENTS_TO_DISPLAY)
      _ <- displayTopBannedIPs(topBannedIps)

      topBannedCountries <- Bans.getTopBannedCountries(locatedBannedIPs, NUMBER_OF_ELEMENTS_TO_DISPLAY)
      _ <- displayTopBannedCountries(topBannedCountries)

      endTime <- currentTime(TimeUnit.MILLISECONDS)
      _ <- putStrLn(s"\n${AnsiColor.GREEN}Analysis completed in ${(endTime - startTime) / 1000f} seconds")
    } yield ()

    program.exitCode
  }

  def findLocationOfIP(geoIP: GeoIP, bannedIP: UnlocatedBannedIP): IO[UnlocatedBannedIP, LocatedBannedIP] = {
    IpConverter.ipv4ToLong(bannedIP.ip).flatMap(geoIP.findCountryOfIP)
      .foldM(
        _       => IO.fail(bannedIP),
        country => IO.succeed(LocatedBannedIP(bannedIP.ip, bannedIP.bansCount, country))
      )
  }

  def displayTopBannedCountries(countries: Seq[BansCountPerCountry]): ZIO[Console, Nothing, Unit] = {
    if (countries.nonEmpty) {
      for {
        _ <- putStrLn(s"\n${AnsiColor.YELLOW}Top banned countries :${AnsiColor.RESET}")
        _ <- ZIO.foreach(countries)(country => putStrLn(s"${country.bansCount} bans : ${country.countryName}"))
      } yield ()
    } else putStrLn(s"\n${AnsiColor.YELLOW}No top banned countries found${AnsiColor.RESET}")
  }

  def displayTopBannedIPs(ips: Seq[BannedIP]): ZIO[Console, Nothing, Unit] = {
    if (ips.nonEmpty) {
      for {
        _ <- putStrLn(s"\n${AnsiColor.YELLOW}Top banned IPs :${AnsiColor.RESET}")
        _ <- ZIO.foreach(ips)(ip => putStrLn(s"${ip.bansCount} bans : ${ip.ip}"))
      } yield ()
    } else putStrLn(s"\n${AnsiColor.YELLOW}No top banned IPs found${AnsiColor.RESET}")
  }

  def displayUnlocatedIPs(ips: Seq[UnlocatedBannedIP]): ZIO[Console, Nothing, Unit] = {
    if (ips.nonEmpty) {
      for {
        _ <- putStrLn(s"\n${AnsiColor.RED}Could not locate following IPs :${AnsiColor.RESET}")
        _ <- ZIO.foreach(ips)(ip => putStrLn(ip.ip))
      } yield ()
    } else ZIO.unit
  }
}
