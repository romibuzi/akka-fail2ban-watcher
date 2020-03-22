package com.romibuzi.fail2banwatcher

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class IpConverterSpec extends AnyFlatSpec with Matchers {
  "ipv4ToLong" should "convert a valid IP to long" in {
    // Given
    val ip = "126.76.98.12"

    // When
    val result = IpConverter.ipv4ToLong(ip)

    // Then
    result shouldBe Some(2118935052)
  }

  "ipv4ToLong" should "not convert an invalid IP" in {
    // Given
    val ip = "126.76.98"

    // When
    val result = IpConverter.ipv4ToLong(ip)

    // Then
    result shouldBe None
  }
}
