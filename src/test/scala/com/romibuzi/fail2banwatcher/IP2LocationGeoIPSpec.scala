package com.romibuzi.fail2banwatcher

import java.io.IOException

import zio.test.Assertion._
import zio.test._
import zio.test.junit.JUnitRunnableSpec
import zio.{ZIO, ZLayer}

class IP2LocationGeoIPSpec extends JUnitRunnableSpec {
  val testGeoIPLayer: ZLayer[Any, Nothing, GeoIP] =
    ZLayer.succeed("ip2location_test.csv") >>> IP2LocationGeoIP.live.orDie

  def spec: Spec[Environment, TestFailure[Unit], TestSuccess] = suite("GeoIPSpec")(
    testM("GeoIP service can identify the country the given IP") {
      // Given
      val targetIP = 17435137L
      val program = for {
        geoIP   <- ZIO.access[GeoIP](_.get)
        country <- ZIO.fromOption(geoIP.findCountryOfIP(targetIP))
      } yield country

      // When
      val result = program.provideLayer(testGeoIPLayer)

      // Then
      assertM(result)(equalTo(Country("AU", "Australia")))
    },

    testM("GeoIP service cannot identify country of the given IP if it's not in the ip2location database") {
      // Given
      val targetIP = 1L
      val program = for {
        geoIP   <- ZIO.access[GeoIP](_.get)
        country <- ZIO.fromOption(geoIP.findCountryOfIP(targetIP)).either
      } yield country

      // When
      val result = program.provideLayer(testGeoIPLayer)

      // Then
      assertM(result)(isLeft)
    },

    testM("GeoIP service cannot be built with unexisting ip2location database") {
      // Given
      val failingLayer = ZLayer.succeed("unexisting.csv") >>> IP2LocationGeoIP.live

      // When / Then
      assertM(failingLayer.launch.run)(fails(isSubtype[IOException](
        hasMessage(containsString("Could not load ip2location database"))
      )))
    }
  )
}
