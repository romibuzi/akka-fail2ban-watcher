package com.romibuzi.fail2banwatcher

import zio.ExitCode
import zio.test.Assertion._
import zio.test._
import zio.test.environment.TestConsole
import zio.test.junit.JUnitRunnableSpec

class Fail2BanWatcherSpec extends JUnitRunnableSpec {
  def spec: Spec[Environment, TestFailure[Nothing], TestSuccess] = suite("Fail2BanWatcherSpec")(
    testM("main run") {
      for {
        exitCode <- Fail2BanWatcher.run(Nil)
        output   <- TestConsole.output
      } yield {
        // refers to src/test/resources/fail2ban_sample.sqlite3 and src/test/resources/ip2location_test.csv
        assert(exitCode)(Assertion.equalTo(ExitCode.success)) &&
          assert(output)(contains("1 bans: 189.115.221.77\n")) &&
          assert(output)(contains("2 bans: 63.142.101.182\n")) &&
          assert(output)(contains("3 bans: 81.151.82.119\n")) &&
          assert(output)(contains("1 bans: Brazil\n")) &&
          assert(output)(contains("2 bans: United States of America\n")) &&
          assert(output)(contains("3 bans: United Kingdom of Great Britain and Northern Ireland\n"))
      }
    },
  )
}
