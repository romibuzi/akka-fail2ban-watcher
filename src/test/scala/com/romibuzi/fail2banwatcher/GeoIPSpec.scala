package com.romibuzi.fail2banwatcher

import zio.ZIO
import zio.test.Assertion._
import zio.test._
import zio.test.junit.JUnitRunnableSpec

class GeoIPSpec extends JUnitRunnableSpec {
  val geoIP: ZIO[Any, Throwable, GeoIP] =
    GeoIP.loadIP2LocationDatabase("ip2location_test.csv")

  def spec: Spec[Environment, TestFailure[Throwable], TestSuccess] = suite("GeoIPSpec")(
    testM("GeoIP client can identify the IP range of the given IP") {
      // Given
      val targetIp = 17435137

      // When
      val result = geoIP.map(_.findIPRangeOfIP(targetIp))

      // Then
      assertM(result)(isSome(equalTo(IPRange(17435136, 17435391, Country("AU", "Australia")))))
    },

    testM("GeoIP client cannot identify the IP range of the given IP if it's not in the ip2location database") {
      // Given
      val targetIp = 1

      // When
      val result = geoIP.map(_.findIPRangeOfIP(targetIp))

      // Then
      assertM(result)(isNone)
    }
  )
}
