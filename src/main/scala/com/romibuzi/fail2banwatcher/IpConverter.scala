package com.romibuzi.fail2banwatcher

import scala.util.Try

object IpConverter {
  def ipv4ToLong(ip: String): Option[Long] = {
    Try(
      ip.split('.')
        .ensuring(_.length == 4, "ip should contains 4 numbers only")
        .map(_.toLong)
        .ensuring(_.forall(number => number >= 0 && number <= 255), "ip numbers should be between 0 and 255")
        .foldLeft(0L)((acc, number) => acc * 256 + number)
    ).toOption
  }
}
