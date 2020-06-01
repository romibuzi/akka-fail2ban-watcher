package com.romibuzi.fail2banwatcher

import zio.test.Assertion._
import zio.test._
import zio.test.junit.JUnitRunnableSpec

class IpConverterSpec extends JUnitRunnableSpec {
  def spec: Spec[Environment, TestFailure[Throwable], TestSuccess] = suite("IpConverterSpec")(
    testM("ipv4ToLong convert a valid IP to long") {
      // Given
      val ip = "126.76.98.12"

      // When
      val result = IpConverter.ipv4ToLong(ip)

      // Then
      assertM(result)(equalTo(2118935052L))
    },

    testM("ipv4ToLong cannot convert an invalid IP") {
      // Given
      val ip = "126.76.98"

      // When
      val result = IpConverter.ipv4ToLong(ip)

      // Then
      assertM(result.run)(fails(isSubtype[AssertionError](anything)))
    }
  )
}
