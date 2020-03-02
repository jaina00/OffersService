package config

import com.typesafe.config.{Config, ConfigFactory}

object Settings {
  private[config] val config: Config = ConfigFactory.load()

  val redisHost = config.getString("redis.host")
  val redisPort = config.getInt("redis.port")

  val minPrice = config.getLong("price.min")
  val minTtl = config.getLong("ttl.min")
}
