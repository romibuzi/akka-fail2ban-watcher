package com.romibuzi.fail2banwatcher

import zio.ZIO
import zio.test.Assertion._
import zio.test._
import zio.test.junit.JUnitRunnableSpec

class BansSpec extends JUnitRunnableSpec {
  def spec: Spec[Environment, TestFailure[Throwable], TestSuccess] = suite("BansSpec")(
    testM("getBannedIPs list IPs and their respective number of bans") {
      // Given
      val program = for {
        repo      <- ZIO.access[BansRepository](_.get)
        bannedIPs <- repo.getBannedIPs
      } yield bannedIPs

      // When
      val result = program.provideLayer(Fail2BanWatcher.bansRepositoryLayer.orDie)

      // Then
      assertM(result)(hasSameElements(Seq(
        // refers to src/test/resources/fail2ban_sample.sqlite3
        UnlocatedBannedIP("81.151.82.119", 3L),
        UnlocatedBannedIP("63.142.101.182", 2L),
        UnlocatedBannedIP("189.115.221.77", 1L)
      )))
    },
  )
}
