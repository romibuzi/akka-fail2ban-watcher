package com.romibuzi.fail2banwatcher

import zio.test.Assertion._
import zio.test._
import zio.test.junit.JUnitRunnableSpec

class IPConverterSpec extends JUnitRunnableSpec {
  def spec: Spec[Environment, TestFailure[Throwable], TestSuccess] = suite("IPConverterSpec")(
    testM("ipv4ToLong convert a valid IP to long") {
      // Given
      val ip = "126.76.98.12"

      // When
      val result = IPConverter.ipv4ToLong(ip)

      // Then
      assertM(result)(equalTo(2118935052L))
    },

    testM("ipv4ToLong cannot convert an invalid IP (3 numbers only)") {
      // Given
      val ip = "126.76.98"

      // When
      val result = IPConverter.ipv4ToLong(ip)

      // Then
      assertM(result.run)(fails(isSubtype[AssertionError](
        hasMessage(containsString("ip should contains 4 numbers only"))
      )))
    },

    testM("ipv4ToLong cannot convert an invalid IP (number more than 255)") {
      // Given
      val ip = "76.98.19.256"

      // When
      val result = IPConverter.ipv4ToLong(ip)

      // Then
      assertM(result.run)(fails(isSubtype[AssertionError](
        hasMessage(containsString("ip numbers should be between 0 and 255"))
      )))
    }
  )
}
