package beyond

import org.apache.curator.RetryPolicy
import org.apache.curator.retry.ExponentialBackoffRetry
import scala.concurrent.duration.Duration
import scala.concurrent.duration.FiniteDuration

object BeyondConfiguration {
  private def configuration = play.api.Play.current.configuration

  def requestTimeout: FiniteDuration =
    Duration(configuration.getString("beyond.request-timeout").get).asInstanceOf[FiniteDuration]

  def mongoDBPath: String = configuration.getString("beyond.mongodb.dbpath").get

  def zooKeeperConfigPath: String = configuration.getString("beyond.zookeeper.config-path").get

  def pluginPaths: Seq[String] = {
    import scala.collection.JavaConverters._
    configuration.getStringList("beyond.plugin.path").map(_.asScala).get
  }

  def curatorConnectionPolicy: RetryPolicy = {
    val curatorPath = "beyond.curator.connection"
    val baseSleepTimeMs = Duration(configuration.getString(s"$curatorPath.base-sleep-time").get).toMillis.toInt
    val maxRetries = configuration.getInt(s"$curatorPath.max-retries").get
    val maxSleepMs = Duration(configuration.getString(s"$curatorPath.max-sleep").get).toMillis.toInt
    new ExponentialBackoffRetry(baseSleepTimeMs, maxRetries, maxSleepMs)
  }

  def currentServerAddress: String = {
    val hostAddress = configuration.getString("http.address").get
    val port = configuration.getInt("http.port").get

    hostAddress + ":" + port.toString
  }
}