package com.romibuzi.fail2banwatcher

import com.typesafe.config.{Config, ConfigFactory}
import slick.interop.zio.DatabaseProvider
import slick.jdbc.JdbcProfile
import zio._
import zio.clock._
import zio.console._

import java.io.FileNotFoundException
import java.nio.file.{Files, Paths}
import java.util.concurrent.TimeUnit
import scala.io.AnsiColor

object Fail2BanWatcher extends App {
  val config: Config = ConfigFactory.load()

  val bansRepositoryLayer: ZLayer[Any, Throwable, BansRepository] = {
    val dbConfigLayer = ZLayer.fromEffect(ZIO.effect(config.getConfig("bans")))
    val dbBackendLayer = ZLayer.succeed[JdbcProfile](slick.jdbc.SQLiteProfile)

    (dbConfigLayer ++ dbBackendLayer) >>> DatabaseProvider.live >>> SlickBansRepository.live
  }

  val ip2locationGeoIPLayer: ZLayer[Any, Throwable, GeoIP] = {
    ZLayer.fromEffect(ZIO.effect(config.getString("ip2location_db_path"))) >>> IP2LocationGeoIP.live
  }

  val program: ZIO[Console with Clock with BansRepository with GeoIP, Throwable, Unit] = for {
    _         <- putStrLn(s"${AnsiColor.BLUE}Starting analysis${AnsiColor.RESET}")
    startTime <- currentTime(TimeUnit.MILLISECONDS)

    db_exists <- ZIO.effect(Files.exists(Paths.get(config.getString("db_path"))))
    _ <- if (!db_exists) ZIO.fail(new FileNotFoundException(s"${config.getString("db_path")} not found")) else ZIO.succeed(Nil)

    nbDisplays <- ZIO.effect(config.getInt("number_of_displays"))
    geoIP      <- ZIO.access[GeoIP](_.get)
    bansRepo   <- ZIO.access[BansRepository](_.get)
    bannedIPs  <- bansRepo.getBannedIPs

    (unlocatedBannedIPs: Seq[UnlocatedBannedIP], locatedBannedIPs: Seq[LocatedBannedIP]) <- ZIO.partition(bannedIPs)(findLocationOfIP(geoIP, _))
    _ <- displayUnlocatedIPs(unlocatedBannedIPs)

    topBannedIps <- ZIO.succeed(bansRepo.getTopBannedIPs(unlocatedBannedIPs ++ locatedBannedIPs, nbDisplays))
    _ <- displayTopBannedIPs(topBannedIps)

    topBannedCountries <- ZIO.succeed(bansRepo.getTopBannedCountries(locatedBannedIPs, nbDisplays))
    _ <- displayTopBannedCountries(topBannedCountries)

    endTime <- currentTime(TimeUnit.MILLISECONDS)
    _ <- putStrLn(s"\n${AnsiColor.GREEN}Analysis completed in ${(endTime - startTime) / 1000f} seconds")
  } yield ()

  def run(args: List[String]): ZIO[ZEnv, Nothing, ExitCode] = {
    program
      .provideCustomLayer(bansRepositoryLayer ++ ip2locationGeoIPLayer)
      .onError(error =>
        putStrLn(s"\n${AnsiColor.RED}Something went wrong: ${error.failures.head.getMessage} ${AnsiColor.RESET}").ignore
      )
      .as(ExitCode.success)
      .orElse(ZIO.succeed(ExitCode.failure))
  }

  def findLocationOfIP(geoIP: GeoIP.Service, bannedIP: UnlocatedBannedIP): IO[UnlocatedBannedIP, LocatedBannedIP] = {
    IPConverter.ipv4ToLong(bannedIP.ip)
      .flatMap(longIP => ZIO.fromOption(geoIP.findCountryOfIP(longIP)))
      .foldM(
        _       => IO.fail(bannedIP),
        country => IO.succeed(LocatedBannedIP(bannedIP.ip, bannedIP.bansCount, country))
      )
  }

  def displayTopBannedCountries(bansPerCountry: Seq[BansCountPerCountry]): ZIO[Console, Nothing, Unit] = {
    if (bansPerCountry.nonEmpty) {
      for {
        _ <- putStrLn(s"\n${AnsiColor.YELLOW}Top banned countries:${AnsiColor.RESET}").orDie
        _ <- ZIO.foreach(bansPerCountry)(bansForCountry => putStrLn(s"${bansForCountry.bansCount} bans: ${bansForCountry.country.name}").orDie)
      } yield ()
    } else putStrLn(s"\n${AnsiColor.YELLOW}No top banned countries found${AnsiColor.RESET}").orDie
  }

  def displayTopBannedIPs(ips: Seq[BannedIP]): ZIO[Console, Nothing, Unit] = {
    if (ips.nonEmpty) {
      for {
        _ <- putStrLn(s"\n${AnsiColor.YELLOW}Top banned IPs:${AnsiColor.RESET}").orDie
        _ <- ZIO.foreach(ips)(ip => putStrLn(s"${ip.bansCount} bans: ${ip.ip}").orDie)
      } yield ()
    } else putStrLn(s"\n${AnsiColor.YELLOW}No top banned IPs found${AnsiColor.RESET}").orDie
  }

  def displayUnlocatedIPs(ips: Seq[UnlocatedBannedIP]): ZIO[Console, Nothing, Unit] = {
    if (ips.nonEmpty) {
      for {
        _ <- putStrLn(s"\n${AnsiColor.RED}Could not locate following IPs:${AnsiColor.RESET}").orDie
        _ <- ZIO.foreach(ips)(ip => putStrLn(ip.ip).orDie)
      } yield ()
    } else ZIO.unit
  }
}
