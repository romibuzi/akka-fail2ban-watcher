package com.romibuzi.fail2banwatcher

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import zio.ZIO

class GeoIPSpec extends AnyFlatSpec with Matchers {
  val geoIP: ZIO[Any, Throwable, GeoIP] =
    GeoIP.loadIP2LocationDatabase("ip2location_test.csv")

  "GeoIP client" should "identify the IP range of the given IP" in {
    // When
    val result = geoIP.map(_.findIPRangeOfIP(targetIp = 17435137))

    // Then
    result.map(
      _ shouldBe Some(IPRange(17435136, 17435391, Country("AU", "Australia")))
    )
  }

  "GeoIP client" should "not identify the IP range of the given IP if it's not in the ip2location database" in {
    // When
    val result = geoIP.map(_.findIPRangeOfIP(targetIp = 1))

    // Then
    result.map(_ shouldBe None)
  }
}
