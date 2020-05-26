package com.romibuzi.fail2banwatcher

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import slick.jdbc.SQLiteProfile
import slick.jdbc.SQLiteProfile.api._

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutor}

class BansSpec extends AnyFlatSpec with Matchers {
  "getNumberOfBansPerIP" should "list IPs and their respecive number of bans" in {
    // Given
    implicit val ec: ExecutionContextExecutor = ExecutionContext.global
    implicit val db: SQLiteProfile.backend.Database = Database.forConfig("bans")

    // When
    val result = Await.result(Bans.getBannedIPs, 1.second)

    // Then
    result should contain theSameElementsAs List(
      UnlocatedBannedIP("81.151.82.119", 3),
      UnlocatedBannedIP("63.142.101.182", 2),
      UnlocatedBannedIP("189.115.221.77", 1)
    )
  }
}
