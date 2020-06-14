package com.romibuzi.fail2banwatcher

import zio.{Task, ZIO}

import scala.util.Try

object IPConverter {
  def ipv4ToLong(ip: String): Task[Long] = {
    ZIO.fromTry(
      Try(
        ip.split('.')
          .ensuring(_.length == 4, "ip should contains 4 numbers only")
          .map(_.toLong)
          .ensuring(_.forall(number => number >= 0 && number <= 255), "ip numbers should be between 0 and 255")
          .foldLeft(0L)((acc, number) => acc * 256 + number)
      )
    )
  }
}
