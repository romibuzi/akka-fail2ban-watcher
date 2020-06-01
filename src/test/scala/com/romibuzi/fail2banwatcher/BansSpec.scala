package com.romibuzi.fail2banwatcher

import slick.jdbc.SQLiteProfile
import slick.jdbc.SQLiteProfile.api._
import zio.test.Assertion._
import zio.test._
import zio.test.junit.JUnitRunnableSpec

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutor}

class BansSpec extends JUnitRunnableSpec {
  def spec: Spec[Environment, TestFailure[Throwable], TestSuccess] = suite("BansSpec")(
    test("getNumberOfBansPerIP list IPs and their respecive number of bans") {
      // Given
      implicit val ec: ExecutionContextExecutor = ExecutionContext.global
      implicit val db: SQLiteProfile.backend.Database = Database.forConfig("bans")

      // When
      val result = Await.result(Bans.getBannedIPs, 1.second)

      // Then
      assert(result)(hasSameElements(List(
        UnlocatedBannedIP("81.151.82.119", 3),
        UnlocatedBannedIP("63.142.101.182", 2),
        UnlocatedBannedIP("189.115.221.77", 1)
      )))
    },
  )
}
